package com.massivecraft.factions.bank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.zcore.util.VersionedFile;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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

    private static final int CURRENT_VERSION = 2;

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
        JsonElement element = VersionedFile.read(this.file, this.logger);
        if (element == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int version = VersionedFile.LEGACY_VERSION;

        if (element.isJsonArray()) {
            // Oldest format: a JSON array of keys without timestamps.
            for (JsonElement item : element.getAsJsonArray()) {
                if (item != null && item.isJsonPrimitive()
                        && item.getAsJsonPrimitive().isString()) {
                    String key = item.getAsString();
                    if (!key.isBlank()) {
                        this.claimedAt.putIfAbsent(key, now);
                    }
                }
            }
        } else if (element.isJsonObject()) {
            JsonObject root = element.getAsJsonObject();
            version = VersionedFile.version(root);
            if (version > CURRENT_VERSION) {
                this.logger.severe("Idempotency store " + this.file + " uses version "
                        + version + " but this build supports " + CURRENT_VERSION
                        + "; leaving it untouched.");
                return;
            }
            Storage storage;
            try {
                storage = this.gson.fromJson(root, Storage.class);
            } catch (RuntimeException exception) {
                this.logger.log(Level.SEVERE,
                        "Could not parse idempotency store " + this.file, exception);
                VersionedFile.quarantine(this.file, this.logger);
                return;
            }
            if (storage != null && storage.entries != null) {
                for (Map.Entry<String, Long> entry : storage.entries.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().isBlank()
                            && entry.getValue() != null) {
                        this.claimedAt.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            // Legacy object of keys without timestamps.
            if (storage != null && storage.keys != null) {
                for (String key : storage.keys) {
                    if (key != null && !key.isBlank()) {
                        this.claimedAt.putIfAbsent(key, now);
                    }
                }
            }
        } else {
            VersionedFile.quarantine(this.file, this.logger);
            return;
        }

        if (version < CURRENT_VERSION) {
            VersionedFile.backup(this.file);
            if (save()) {
                this.logger.info("Migrated idempotency store " + this.file
                        + " from version " + version + " to " + CURRENT_VERSION);
            } else {
                this.logger.warning("Could not migrate idempotency store " + this.file
                        + " to version " + CURRENT_VERSION);
            }
        }
    }

    private boolean save() {
        return VersionedFile.write(this.file, this.gson.toJson(new Storage(this.claimedAt)),
                this.logger);
    }

    /**
     * On-disk shape. Gson needs mutable fields and a no-argument constructor.
     * {@code keys} is read-only compatibility for files written before timestamps.
     */
    private static final class Storage {
        int version = CURRENT_VERSION;
        Map<String, Long> entries = new LinkedHashMap<>();
        Set<String> keys;

        Storage() {
        }

        Storage(Map<String, Long> entries) {
            this.entries = new LinkedHashMap<>(entries);
        }
    }
}
