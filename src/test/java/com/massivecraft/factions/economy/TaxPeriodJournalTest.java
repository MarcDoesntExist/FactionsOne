package com.massivecraft.factions.economy;

import com.massivecraft.factions.bank.IdempotencyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for tax period recovery and reconciliation.
 *
 * @author MarcDoesntExists
 */
class TaxPeriodJournalTest {

    @TempDir
    Path directory;

    @Test
    void settledAbortedAndUnknownSurviveReload() {
        Path file = file();
        TaxPeriodJournal journal = journal(file);
        assertTrue(journal.begin("tax:f1:1", "f1", 10.0, 1L));
        assertTrue(journal.settled("tax:f1:1", "paid"));
        assertTrue(journal.begin("tax:f1:2", "f1", 10.0, 2L));
        assertTrue(journal.abort("tax:f1:2", "NO_FUNDS"));
        assertTrue(journal.begin("tax:f1:3", "f1", 10.0, 3L));
        assertTrue(journal.unknown("tax:f1:3", "crash"));

        TaxPeriodJournal replayed = journal(file);
        assertEquals(TaxPeriodJournal.State.SETTLED, replayed.entry("tax:f1:1").state());
        assertEquals(TaxPeriodJournal.State.ABORTED, replayed.entry("tax:f1:2").state());
        assertEquals(TaxPeriodJournal.State.TAX_UNKNOWN, replayed.entry("tax:f1:3").state());
        assertEquals(10.0, replayed.entry("tax:f1:3").amount());
    }

    @Test
    void recoveryMarksClaimedPeriodUnknownAndUnclaimedAborted() {
        Path file = file();
        IdempotencyStore idempotency = idempotency();
        TaxPeriodJournal journal = journal(file);
        assertTrue(journal.begin("tax:f1:1", "f1", 10.0, 1L));
        assertTrue(idempotency.claim("tax:f1:1"));
        assertTrue(journal.begin("tax:f1:2", "f1", 10.0, 2L));

        // Simulates a restart after the claim and before the debit.
        TaxPeriodJournal recovered = journal(file);
        assertEquals(1, recovered.recover(idempotency));
        assertEquals(TaxPeriodJournal.State.TAX_UNKNOWN, recovered.entry("tax:f1:1").state());
        assertEquals(TaxPeriodJournal.State.ABORTED, recovered.entry("tax:f1:2").state());

        TaxPeriodJournal afterRestart = journal(file);
        assertEquals(0, afterRestart.recover(idempotency));
        assertEquals(TaxPeriodJournal.State.TAX_UNKNOWN, afterRestart.entry("tax:f1:1").state());
        assertEquals("tax:f1:1", afterRestart.latestUnknown("f1").key());
    }

    @Test
    void unknownPeriodIsOnlySettledExplicitly() {
        Path file = file();
        IdempotencyStore idempotency = idempotency();
        TaxPeriodJournal journal = journal(file);
        assertTrue(journal.begin("tax:f1:1", "f1", 10.0, 1L));
        assertTrue(idempotency.claim("tax:f1:1"));

        TaxPeriodJournal recovered = journal(file);
        assertEquals(1, recovered.recover(idempotency));
        assertEquals(0, recovered.recover(idempotency));
        assertEquals(TaxPeriodJournal.State.TAX_UNKNOWN, recovered.entry("tax:f1:1").state());

        assertTrue(recovered.settled("tax:f1:1", "manual retry"));
        TaxPeriodJournal after = journal(file);
        assertEquals(TaxPeriodJournal.State.SETTLED, after.entry("tax:f1:1").state());
        assertEquals(0, after.recover(idempotency));
        assertEquals(TaxPeriodJournal.State.SETTLED, after.entry("tax:f1:1").state());
    }

    @Test
    void resolvedPeriodsAreSkippedByTheRunner() {
        assertFalse(FactionTaxService.isResolved(null));
        assertFalse(FactionTaxService.isResolved(entry(TaxPeriodJournal.State.PENDING)));
        assertFalse(FactionTaxService.isResolved(entry(TaxPeriodJournal.State.ABORTED)));
        assertTrue(FactionTaxService.isResolved(entry(TaxPeriodJournal.State.SETTLED)));
        assertTrue(FactionTaxService.isResolved(entry(TaxPeriodJournal.State.TAX_UNKNOWN)));
    }

    @Test
    void malformedLinesAreIgnored() throws IOException {
        Path file = file();
        TaxPeriodJournal journal = journal(file);
        assertTrue(journal.begin("tax:f1:1", "f1", 10.0, 1L));
        Files.writeString(file, "not-json\n", StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);

        TaxPeriodJournal replayed = journal(file);
        assertNotNull(replayed.entry("tax:f1:1"));
        assertEquals(TaxPeriodJournal.State.PENDING, replayed.entry("tax:f1:1").state());
    }

    private Path file() {
        return this.directory.resolve("tax-journal.jsonl");
    }

    private TaxPeriodJournal journal(Path file) {
        return new TaxPeriodJournal(file, Logger.getLogger("test"));
    }

    private IdempotencyStore idempotency() {
        return new IdempotencyStore(this.directory.resolve("idempotency.json"),
                Logger.getLogger("test"));
    }

    private static TaxPeriodJournal.Entry entry(TaxPeriodJournal.State state) {
        return new TaxPeriodJournal.Entry("tax:f1:1", "f1", 10.0, 1L, state, "");
    }
}
