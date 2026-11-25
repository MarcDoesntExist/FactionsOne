package com.massivecraft.factions.bank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Durable claims for idempotent operations (for example {@code tax:...} or
 * {@code mailbox:...}). Claims carry their creation time so old entries can be
 * compacted without touching recent or in-flight work.
 *
 * <p>The store is owned by one server process; its methods are serialized.
 *
 * @author MarcDoesntExists
 */
public final class IdempotencyStore {

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().create();
    private final Map<String, Long> claimedAt = new LinkedHashMap<>();

    public IdempotencyStore(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    public synchronized boolean claim(String key) {
        return claimAt(key, System.currentTimeMillis());
    }

    synchronized boolean claimAt(String key, long at) {
        if (key == null || key.isBlank() || this.claimedAt.containsKey(key)) {
            return false;
        }
        this.claimedAt.put(key, at);
        if (!save()) {
            this.claimedAt.remove(key);
            return false;
        }
        return true;
    }

    public synchronized void release(String key) {
        if (key != null && this.claimedAt.remove(key) != null) {
            save();
        }
    }

    public synchronized boolean isClaimed(String key) {
        return key != null && this.claimedAt.containsKey(key);
    }

    /**
     * Removes claims older than the window and rewrites the file. Recent claims
     * and claims created after {@code now} are kept. Returns how many keys were
     * removed.
     */
    public synchronized int retain(long now, long retentionMillis) {
        return retain(now, retentionMillis, key -> false);
    }

    /**
     * Compacts old claims except those matched by {@code protect}. The mailbox
     * uses this to keep keys for pending, ambiguous and unrefunded claims.
     */
    public synchronized int retain(long now, long retentionMillis, Predicate<String> protect) {
        long cutoff = now - Math.max(0L, retentionMillis);
        int before = this.claimedAt.size();
        this.claimedAt.entrySet().removeIf(entry -> entry.getValue() <= cutoff
                && !protect.test(entry.getKey()));
        int removed = before - this.claimedAt.size();
        if (removed > 0) {
            save();
        }
        return removed;
    }

    public synchronized int size() {
        return this.claimedAt.size();
    }

    public Path file() {
        return this.file;
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try {
            Storage storage = this.gson.fromJson(
                    Files.readString(this.file, StandardCharsets.UTF_8), Storage.class);
            if (storage == null) {
                return;
            }
            long now = System.currentTimeMillis();
            if (storage.entries != null) {
                for (Map.Entry<String, Long> entry : storage.entries.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().isBlank()
                            && entry.getValue() != null) {
                        this.claimedAt.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            // Old format: a JSON array of keys without timestamps.
            if (storage.keys != null) {
                for (String key : storage.keys) {
                    if (key != null && !key.isBlank()) {
                        this.claimedAt.putIfAbsent(key, now);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Could not read idempotency store " + this.file, exception);
        }
    }

    private boolean save() {
        Path temporary = this.file.resolveSibling(
                this.file.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(temporary, this.gson.toJson(new Storage(this.claimedAt)),
                    StandardCharsets.UTF_8);
            try {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not write idempotency store " + this.file, exception);
            return false;
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // The next save uses a fresh temporary name.
            }
        }
    }

    /**
     * On-disk shape. Gson needs mutable fields and a no-argument constructor.
     * {@code keys} is read-only compatibility for files written before timestamps.
     */
    private static final class Storage {
        Map<String, Long> entries = new LinkedHashMap<>();
        Set<String> keys;

        Storage() {
        }

        Storage(Map<String, Long> entries) {
            this.entries = new LinkedHashMap<>(entries);
        }
    }
}
