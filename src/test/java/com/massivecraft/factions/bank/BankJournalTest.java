package com.massivecraft.factions.bank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Headless checks for the durable bank journal.
 *
 * @author MarcDoesntExists
 */
class BankJournalTest {

    @TempDir
    Path directory;

    @Test
    void committedMovementSurvivesReplay() {
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = new BankJournal(file, Logger.getLogger("test"));
        String id = journal.begin("transfer", "a", "b", 10.0, "tester", "a", 10.0, "test");
        assertNotNull(id);
        assertEquals(BankJournal.State.PENDING, journal.entry(id).state());

        journal.commit(id, "ok");
        BankJournal replayed = new BankJournal(file, Logger.getLogger("test"));
        assertEquals(BankJournal.State.COMMITTED, replayed.entry(id).state());
        assertEquals(10.0, replayed.entry(id).amount());
    }

    @Test
    void compensationPendingIsRetriedAndSurvivesReplay() {
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = new BankJournal(file, Logger.getLogger("test"));
        String id = journal.begin("transfer", "a", "b", 20.0, "tester", "a", 20.0, "test");
        journal.compensationPending(id, "refund failed");

        assertEquals(1, journal.pendingCompensationCount());
        BankJournal replayed = new BankJournal(file, Logger.getLogger("test"));
        assertEquals(1, replayed.pendingCompensations().size());

        replayed.compensated(id, "retry");
        assertEquals(0, replayed.pendingCompensationCount());
        assertEquals(BankJournal.State.COMPENSATED,
                new BankJournal(file, Logger.getLogger("test")).entry(id).state());
    }

    @Test
    void malformedLinesAreIgnored() throws IOException {
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = new BankJournal(file, Logger.getLogger("test"));
        String id = journal.begin("credit", "", "a", 5.0, "system", null, 0.0, "test");
        Files.writeString(file, "not-json\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        BankJournal replayed = new BankJournal(file, Logger.getLogger("test"));
        assertEquals(BankJournal.State.PENDING, replayed.entry(id).state());
    }

    @Test
    void unknownIdCannotBeTransitioned() {
        BankJournal journal = new BankJournal(this.directory.resolve("journal.jsonl"),
                Logger.getLogger("test"));
        assertEquals(false, journal.commit("missing", "ok"));
        assertEquals(false, journal.compensated("missing", "ok"));
    }
}
