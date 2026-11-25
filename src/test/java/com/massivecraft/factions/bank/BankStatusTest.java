package com.massivecraft.factions.bank;

import com.massivecraft.factions.mailbox.FactionMailbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for the operator status snapshot.
 *
 * @author MarcDoesntExists
 */
class BankStatusTest {

    @TempDir
    Path directory;

    @Test
    void emptyStateIsAllZero() {
        assertEquals(new BankStatus(0, 0, 0, 0, 0), BankStatus.of(null, null));

        MutableClock clock = new MutableClock();
        BankJournal journal = new BankJournal(this.directory.resolve("journal.jsonl"),
                Logger.getLogger("test"), clock);
        FactionMailbox mailbox = new FactionMailbox(this.directory.resolve("mailbox.json"),
                idempotency(), clock, 5, 1_000L, Logger.getLogger("test"));
        assertEquals(new BankStatus(0, 0, 0, 0, 0), BankStatus.of(journal, mailbox));
    }

    @Test
    void countsEachAmbiguousStateSeparatelyAndNeverWrites() throws IOException {
        MutableClock clock = new MutableClock();
        Logger logger = Logger.getLogger("test");
        Path journalFile = this.directory.resolve("journal.jsonl");
        BankJournal journal = new BankJournal(journalFile, logger, clock);
        journal.begin("credit", "", "a", 1.0, "system", null, 0.0, "pending movement");
        String compensation = journal.begin("transfer", "a", "b", 2.0,
                "tester", "a", 2.0, "compensation");
        assertTrue(journal.compensationPending(compensation, "refund failed"));
        String committed = journal.begin("credit", "", "a", 3.0, "system", null, 0.0, "done");
        assertTrue(journal.commit(committed, "ok"));

        Path mailboxFile = this.directory.resolve("mailbox.json");
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = new FactionMailbox(mailboxFile, idempotency, clock,
                5, 1_000L, logger);
        mailbox.enqueue("unknown-delivery", "player2", 5.0, "source", "test", "delivery");
        assertTrue(idempotency.claim("mailbox:unknown-delivery"));
        mailbox = new FactionMailbox(mailboxFile, idempotency, clock, 5, 1_000L, logger);

        mailbox.enqueue("refund", "player3", 5.0, "source", "test", "refund");
        clock.advance(Duration.ofSeconds(2));
        FactionMailbox crashing = mailbox;
        assertThrows(SimulatedCrash.class, () -> crashing.expireStale(ignored -> {
            throw new SimulatedCrash();
        }));
        mailbox = new FactionMailbox(mailboxFile, idempotency, clock, 5, 1_000L, logger);
        mailbox.enqueue("pending", "player1", 5.0, "source", "test", "pending");

        String journalBefore = Files.readString(journalFile);
        String mailboxBefore = Files.readString(mailboxFile);
        BankStatus status = BankStatus.of(journal, mailbox);

        assertEquals(new BankStatus(1, 1, 1, 1, 1), status);
        assertEquals(1, status.pendingJournal());
        assertEquals(1, status.pendingCompensations());
        assertEquals(1, status.pendingMailbox());
        assertEquals(1, status.unknownDeliveries());
        assertEquals(1, status.unknownRefunds());
        assertEquals(journalBefore, Files.readString(journalFile));
        assertEquals(mailboxBefore, Files.readString(mailboxFile));
        assertEquals(status, BankStatus.of(journal, mailbox));
    }

    private IdempotencyStore idempotency() {
        return new IdempotencyStore(this.directory.resolve("idempotency.json"),
                Logger.getLogger("test"));
    }

    private static final class SimulatedCrash extends Error {
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
