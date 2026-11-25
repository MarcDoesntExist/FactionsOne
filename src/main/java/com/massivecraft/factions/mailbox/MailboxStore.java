package com.massivecraft.factions.mailbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.zcore.util.VersionedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Atomic JSON persistence for {@link FactionMailbox}. Kept separate from the
 * delivery/reconciliation rules so the mailbox stays under the project's
 * per-file limit.
 *
 * <p>The file carries a {@code version} field. Unversioned files are migrated
 * after a rotated backup; a corrupt file is quarantined and the mailbox starts
 * empty instead of overwriting the evidence.</p>
 *
 * @author MarcDoesntExists
 */
final class MailboxStore {

    static final int CURRENT_VERSION = 2;

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().create();

    MailboxStore(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    synchronized Map<String, FactionMailbox.Claim> load() {
        Map<String, FactionMailbox.Claim> claims = new LinkedHashMap<>();
        JsonElement element = VersionedFile.read(this.file, this.logger);
        if (element == null) {
            return claims;
        }
        if (!element.isJsonObject()) {
            VersionedFile.quarantine(this.file, this.logger);
            return claims;
        }

        JsonObject root = element.getAsJsonObject();
        int version = VersionedFile.version(root);
        if (version > CURRENT_VERSION) {
            this.logger.severe("Mailbox " + this.file + " uses version " + version
                    + " but this build supports " + CURRENT_VERSION
                    + "; leaving it untouched.");
            return claims;
        }

        Storage storage;
        try {
            storage = this.gson.fromJson(root, Storage.class);
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Could not parse mailbox " + this.file, exception);
            VersionedFile.quarantine(this.file, this.logger);
            return claims;
        }
        if (storage != null && storage.claims != null) {
            for (ClaimData data : storage.claims) {
                FactionMailbox.Claim claim = data == null ? null : data.toClaim();
                if (claim != null) {
                    claims.put(claim.id(), claim);
                }
            }
        }

        if (version < CURRENT_VERSION) {
            VersionedFile.backup(this.file);
            if (save(claims.values())) {
                this.logger.info("Migrated mailbox " + this.file + " from version "
                        + version + " to " + CURRENT_VERSION);
            } else {
                this.logger.warning("Could not migrate mailbox " + this.file
                        + " to version " + CURRENT_VERSION);
            }
        }
        return claims;
    }

    synchronized boolean save(Collection<FactionMailbox.Claim> claims) {
        Storage storage = new Storage();
        storage.claims = new ArrayList<>();
        for (FactionMailbox.Claim claim : claims) {
            storage.claims.add(ClaimData.from(claim));
        }
        return VersionedFile.write(this.file, this.gson.toJson(storage), this.logger);
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
        int version = CURRENT_VERSION;
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
