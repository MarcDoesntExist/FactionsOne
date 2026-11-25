package com.massivecraft.factions.bank;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Compacts the durable bank journal, the idempotency store and the mailbox.
 *
 * <p>Retention only removes terminal records older than the configured
 * windows; pending or ambiguous work is always kept. The method returns the
 * total number of removed records so the caller can log one line.
 *
 * Extracted from {@link Main}.
 *
 * @author MarcDoesntExists
 */
public final class StorageRetention {

    private static volatile long lastCompactedAt;

    private StorageRetention() {
    }

    /** Millisecond timestamp of the last compaction since startup, or zero. */
    public static long lastCompactedAt() {
        return lastCompactedAt;
    }

    public static int compact(Main main) {
        if (main == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        Set<String> protectedKeys = main.mailbox == null
                ? Set.of() : main.mailbox.protectedKeys();
        int keys = main.idempotencyStore == null ? 0 : main.idempotencyStore.retain(now,
                TimeUnit.DAYS.toMillis(Math.max(1, Conf.idempotencyRetentionDays)),
                protectedKeys::contains);
        int claims = main.mailbox == null ? 0 : main.mailbox.retain(now,
                TimeUnit.DAYS.toMillis(Math.max(1, Conf.mailboxRetentionDays)));
        int entries = main.bankJournal == null ? 0 : main.bankJournal.retain(now,
                TimeUnit.DAYS.toMillis(Math.max(1, Conf.bankJournalRetentionDays)));
        int removed = keys + claims + entries;
        lastCompactedAt = System.currentTimeMillis();
        if (removed > 0) {
            main.log("Storage retention removed " + keys + " idempotency key(s), "
                    + claims + " mailbox claim(s) and " + entries + " journal entry(ies).");
        }
        return removed;
    }
}
