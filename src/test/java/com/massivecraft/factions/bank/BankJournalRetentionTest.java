package com.massivecraft.factions.bank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for bank journal retention.
 *
 * @author MarcDoesntExists
 */
class BankJournalRetentionTest {

    @TempDir
    Path directory;

    @Test
    void removesOnlyTerminalEntriesOlderThanTheWindow() {
        MutableClock clock = new MutableClock();
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = journal(file, clock);
        String committed = commit(journal, "old committed");
        String aborted = journal.begin("credit", "", "a", 4.0, "system", null, 0.0, "old");
        assertTrue(journal.abort(aborted, "cancelled"));
        String compensated = compensated(journal, "old compensated");

        clock.advance(Duration.ofDays(40));
        String recent = commit(journal, "recent committed");
        String pending = journal.begin("credit", "", "a", 2.0, "system", null, 0.0, "pending");
        String pendingCompensation = journal.begin("transfer", "a", "b", 3.0,
                "tester", "a", 3.0, "pending compensation");
        assertTrue(journal.compensationPending(pendingCompensation, "refund failed"));

        assertEquals(3, journal.retain(clock.millis(), Duration.ofDays(30).toMillis()));
        assertNull(journal.entry(committed));
        assertNull(journal.entry(aborted));
        assertNull(journal.entry(compensated));
        assertNotNull(journal.entry(recent));
        assertNotNull(journal.entry(pending));
        assertNotNull(journal.entry(pendingCompensation));

        BankJournal replayed = journal(file, clock);
        assertNull(replayed.entry(committed));
        assertNull(replayed.entry(aborted));
        assertNull(replayed.entry(compensated));
        assertEquals(BankJournal.State.COMMITTED, replayed.entry(recent).state());
        assertEquals(BankJournal.State.PENDING, replayed.entry(pending).state());
        assertEquals(1, replayed.pendingCompensationCount());
    }

    @Test
    void pendingAndCompensationSurviveAnyWindow() {
        MutableClock clock = new MutableClock();
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = journal(file, clock);
        String pending = journal.begin("credit", "", "a", 2.0, "system", null, 0.0, "pending");
        String compensation = journal.begin("transfer", "a", "b", 3.0,
                "tester", "a", 3.0, "compensation");
        assertTrue(journal.compensationPending(compensation, "refund failed"));

        clock.advance(Duration.ofDays(400));
        assertEquals(0, journal.retain(clock.millis(), Duration.ofDays(30).toMillis()));
        assertNotNull(journal.entry(pending));
        assertNotNull(journal.entry(compensation));

        BankJournal replayed = journal(file, clock);
        assertEquals(BankJournal.State.PENDING, replayed.entry(pending).state());
        assertEquals(1, replayed.pendingCompensationCount());
    }

    @Test
    void futureEntriesAreKeptWhenTheClockMovesBackwards() {
        MutableClock clock = new MutableClock();
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = journal(file, clock);
        String committed = commit(journal, "future");

        long earlier = clock.millis() - Duration.ofDays(1).toMillis();
        assertEquals(0, journal.retain(earlier, Duration.ofDays(30).toMillis()));
        assertNotNull(journal.entry(committed));
    }

    @Test
    void malformedLinesDoNotBlockCompaction() throws IOException {
        MutableClock clock = new MutableClock();
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = journal(file, clock);
        String committed = commit(journal, "old");
        Files.writeString(file, "not-json\n", StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);

        clock.advance(Duration.ofDays(40));
        assertEquals(1, journal.retain(clock.millis(), Duration.ofDays(30).toMillis()));
        assertFalse(Files.readString(file).contains("not-json"));
        assertNull(journal(file, clock).entry(committed));
    }

    @Test
    void rewriteTakesARotatedBackup() throws IOException {
        MutableClock clock = new MutableClock();
        Path file = this.directory.resolve("journal.jsonl");
        BankJournal journal = journal(file, clock);
        String committed = commit(journal, "old");

        clock.advance(Duration.ofDays(40));
        assertEquals(1, journal.retain(clock.millis(), Duration.ofDays(30).toMillis()));

        Path backups = this.directory.resolve("backups");
        assertTrue(Files.isDirectory(backups));
        List<Path> files;
        try (Stream<Path> stream = Files.list(backups)) {
            files = stream.toList();
        }
        assertEquals(1, files.size());
        assertTrue(Files.readString(files.get(0)).contains(committed));
    }

    private BankJournal journal(Path file, Clock clock) {
        return new BankJournal(file, Logger.getLogger("test"), clock);
    }

    private static String commit(BankJournal journal, String detail) {
        String id = journal.begin("credit", "", "a", 1.0, "system", null, 0.0, detail);
        journal.commit(id, detail);
        return id;
    }

    private static String compensated(BankJournal journal, String detail) {
        String id = journal.begin("transfer", "a", "b", 3.0, "tester", "a", 3.0, detail);
        journal.compensationPending(id, detail);
        journal.compensated(id, detail);
        return id;
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }
}
