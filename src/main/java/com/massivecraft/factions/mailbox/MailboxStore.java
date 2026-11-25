package com.massivecraft.factions.mailbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Atomic JSON persistence for {@link FactionMailbox}. Kept separate from the
 * delivery/reconciliation rules so the mailbox stays under the project's
 * per-file limit.
 *
 * @author MarcDoesntExists
 */
final class MailboxStore {

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().create();

    MailboxStore(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    synchronized Map<String, FactionMailbox.Claim> load() {
        Map<String, FactionMailbox.Claim> claims = new LinkedHashMap<>();
        if (!Files.exists(this.file)) {
            return claims;
        }
        try {
            Storage storage = this.gson.fromJson(
                    Files.readString(this.file, StandardCharsets.UTF_8), Storage.class);
            if (storage == null || storage.claims == null) {
                return claims;
            }
            for (ClaimData data : storage.claims) {
                FactionMailbox.Claim claim = data == null ? null : data.toClaim();
                if (claim != null) {
                    claims.put(claim.id(), claim);
                }
            }
        } catch (IOException | RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Could not read mailbox " + this.file, exception);
        }
        return claims;
    }

    synchronized boolean save(Collection<FactionMailbox.Claim> claims) {
        Path temporary = this.file.resolveSibling(
                this.file.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Storage storage = new Storage();
            storage.claims = new ArrayList<>();
            for (FactionMailbox.Claim claim : claims) {
                storage.claims.add(ClaimData.from(claim));
            }
            Files.writeString(temporary, this.gson.toJson(storage), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not write mailbox " + this.file, exception);
            return false;
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // A fresh temporary name is used on the next save.
            }
        }
    }

    /**
     * Drops delivered claims and fully refunded expired claims older than the
     * window, leaving pending work and ambiguous deliveries or refunds
     * untouched.
     */
    synchronized int retain(Map<String, FactionMailbox.Claim> claims, long now,
                            long retentionMillis) {
        long cutoff = now - Math.max(0L, retentionMillis);
        int before = claims.size();
        claims.entrySet().removeIf(entry -> removable(entry.getValue(), cutoff));
        int removed = before - claims.size();
        if (removed > 0) {
            save(claims.values());
        }
        return removed;
    }

    private static boolean removable(FactionMailbox.Claim claim, long cutoff) {
        if (claim.state() == FactionMailbox.State.DELIVERED) {
            return completedAt(claim) <= cutoff;
        }
        return claim.state() == FactionMailbox.State.EXPIRED && claim.refunded()
                && completedAt(claim) <= cutoff;
    }

    private static long completedAt(FactionMailbox.Claim claim) {
        return claim.completedAt() > 0L ? claim.completedAt()
                : Math.max(claim.createdAt(), claim.expiresAt());
    }

    Path file() {
        return this.file;
    }

    private static final class Storage {
        List<ClaimData> claims = new ArrayList<>();
    }

    private static final class ClaimData {
        String id;
        String playerId;
        double amount;
        String sourceAccount;
        String source;
        String reason;
        long createdAt;
        long expiresAt;
        String state;
        boolean refunded;
        long completedAt;

        static ClaimData from(FactionMailbox.Claim claim) {
            ClaimData data = new ClaimData();
            data.id = claim.id();
            data.playerId = claim.playerId();
            data.amount = claim.amount();
            data.sourceAccount = claim.sourceAccount();
            data.source = claim.source();
            data.reason = claim.reason();
            data.createdAt = claim.createdAt();
            data.expiresAt = claim.expiresAt();
            data.state = claim.state().name();
            data.refunded = claim.refunded();
            data.completedAt = claim.completedAt();
            return data;
        }

        FactionMailbox.Claim toClaim() {
            if (this.id == null || this.id.isBlank() || this.playerId == null
                    || this.playerId.isBlank() || this.state == null) {
                return null;
            }
            FactionMailbox.State parsed;
            try {
                parsed = FactionMailbox.State.valueOf(this.state);
            } catch (IllegalArgumentException invalidState) {
                return null;
            }
            return new FactionMailbox.Claim(this.id, this.playerId, this.amount,
                    nullToEmpty(this.sourceAccount), nullToEmpty(this.source), nullToEmpty(this.reason),
                    this.createdAt, this.expiresAt, parsed, this.refunded, this.completedAt);
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
