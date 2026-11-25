package com.massivecraft.factions.mailbox;

import com.massivecraft.factions.bank.IdempotencyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for conservative refund recovery.
 *
 * @author MarcDoesntExists
 */
class MailboxRefundTest {

    @TempDir
    Path directory;

    @Test
    void crashAfterRefundKeyIsUnknownAndNotRetried() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0,
                "source", "test", "why");
        clock.advance(Duration.ofSeconds(2));

        // The claim is persisted as EXPIRED, then the refund key is claimed and
        // the process dies before the credit: simulate with an Error, which the
        // refund path deliberately does not catch.
        assertThrows(SimulatedCrash.class, () -> mailbox.expireStale(ignored -> {
            throw new SimulatedCrash();
        }));
        assertTrue(idempotency.isClaimed("mailbox-refund:" + claim.id()));
        assertFalse(mailbox.get(claim.id()).refunded());

        FactionMailbox recovered = mailbox(clock, idempotency);
        assertEquals(FactionMailbox.State.REFUND_UNKNOWN, recovered.get(claim.id()).state());
        assertFalse(recovered.get(claim.id()).refunded());
        assertEquals(1, recovered.unknownRefunds().size());
        assertTrue(recovered.protectedKeys().contains("mailbox-refund:" + claim.id()));

        AtomicInteger refunds = new AtomicInteger();
        assertEquals(0, recovered.expireStale(ignored -> {
            refunds.incrementAndGet();
            return true;
        }));
        assertEquals(0, refunds.get(), "an ambiguous refund must not be retried automatically");
    }

    @Test
    void retryRefundReleasesTheKeyAndCreditsOnce() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0,
                "source", "test", "why");
        clock.advance(Duration.ofSeconds(2));
        assertThrows(SimulatedCrash.class, () -> mailbox.expireStale(ignored -> {
            throw new SimulatedCrash();
        }));
        FactionMailbox recovered = mailbox(clock, idempotency);
        assertEquals(FactionMailbox.State.REFUND_UNKNOWN, recovered.get(claim.id()).state());

        assertTrue(recovered.resolveRefundUnknown(claim.id(), false));
        assertFalse(idempotency.isClaimed("mailbox-refund:" + claim.id()));
        assertEquals(FactionMailbox.State.EXPIRED, recovered.get(claim.id()).state());
        assertFalse(recovered.get(claim.id()).refunded());

        AtomicInteger refunds = new AtomicInteger();
        recovered.expireStale(ignored -> {
            refunds.incrementAndGet();
            return true;
        });
        assertEquals(1, refunds.get());
        assertTrue(recovered.get(claim.id()).refunded());
        recovered.expireStale(ignored -> {
            refunds.incrementAndGet();
            return true;
        });
        assertEquals(1, refunds.get(), "a completed refund must not run twice");
    }

    @Test
    void reconcileAsRefundedDoesNotCallTheSink() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0,
                "source", "test", "why");
        clock.advance(Duration.ofSeconds(2));
        assertThrows(SimulatedCrash.class, () -> mailbox.expireStale(ignored -> {
            throw new SimulatedCrash();
        }));
        FactionMailbox recovered = mailbox(clock, idempotency);

        assertTrue(recovered.resolveRefundUnknown(claim.id(), true));
        assertEquals(FactionMailbox.State.EXPIRED, recovered.get(claim.id()).state());
        assertTrue(recovered.get(claim.id()).refunded());
        AtomicInteger refunds = new AtomicInteger();
        recovered.expireStale(ignored -> {
            refunds.incrementAndGet();
            return true;
        });
        assertEquals(0, refunds.get());
        assertTrue(recovered.unknownRefunds().isEmpty());
    }

    @Test
    void refundUnknownIsKeptByRetentionAndSurvivesReload() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0,
                "source", "test", "why");
        clock.advance(Duration.ofSeconds(2));
        assertThrows(SimulatedCrash.class, () -> mailbox.expireStale(ignored -> {
            throw new SimulatedCrash();
        }));
        FactionMailbox recovered = mailbox(clock, idempotency);

        clock.advance(Duration.ofDays(40));
        assertEquals(0, recovered.retain(clock.millis(), Duration.ofDays(30).toMillis()));
        assertNotNull(recovered.get(claim.id()), "an ambiguous refund must survive retention");
        assertTrue(recovered.protectedKeys().contains("mailbox-refund:" + claim.id()));

        FactionMailbox replayed = mailbox(clock, idempotency);
        assertEquals(FactionMailbox.State.REFUND_UNKNOWN, replayed.get(claim.id()).state());
        assertNull(replayed.get("missing"));
    }

    private FactionMailbox mailbox(Clock clock, IdempotencyStore idempotency) {
        return new FactionMailbox(this.directory.resolve("mailbox.json"), idempotency, clock,
                5, 1_000L, Logger.getLogger("test"));
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
