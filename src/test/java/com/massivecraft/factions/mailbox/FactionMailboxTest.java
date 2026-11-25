package com.massivecraft.factions.mailbox;

import com.massivecraft.factions.bank.IdempotencyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for the durable mailbox.
 *
 * @author MarcDoesntExists
 */
class FactionMailboxTest {

    private static final int THREADS = 8;

    @TempDir
    Path directory;

    @Test
    void claimIsDeliveredOnceAndSurvivesRestart() {
        MutableClock clock = new MutableClock();
        FactionMailbox mailbox = mailbox(clock, 5, 60_000L);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0, "source", "test", "why");
        assertNotNull(claim);

        AtomicInteger deliveries = new AtomicInteger();
        FactionMailbox.Delivery delivery = ignored -> {
            deliveries.incrementAndGet();
            return true;
        };
        assertEquals(FactionMailbox.Result.SUCCESS,
                mailbox.claim("player", claim.id(), delivery, ignored -> true));
        assertEquals(FactionMailbox.Result.ALREADY_CLAIMED,
                mailbox.claim("player", claim.id(), delivery, ignored -> true));
        assertEquals(1, deliveries.get());
        assertTrue(mailbox.pendingFor("player").isEmpty());

        FactionMailbox replayed = mailbox(clock, 5, 60_000L);
        assertEquals(FactionMailbox.State.DELIVERED, replayed.get(claim.id()).state());
        assertEquals(FactionMailbox.Result.ALREADY_CLAIMED,
                replayed.claim("player", claim.id(), delivery, ignored -> true));
        assertEquals(1, deliveries.get());
    }

    @Test
    void failedDeliveryStaysPendingAndCanBeRetried() {
        FactionMailbox mailbox = mailbox(new MutableClock(), 5, 60_000L);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0, "source", "test", "why");

        assertEquals(FactionMailbox.Result.DELIVERY_FAILED,
                mailbox.claim("player", claim.id(), ignored -> false, ignored -> true));
        assertEquals(1, mailbox.pendingFor("player").size());

        assertEquals(FactionMailbox.Result.SUCCESS,
                mailbox.claim("player", claim.id(), ignored -> true, ignored -> true));
        assertTrue(mailbox.pendingFor("player").isEmpty());
    }

    @Test
    void concurrentClaimsHaveExactlyOneWinner() throws Exception {
        FactionMailbox mailbox = mailbox(new MutableClock(), 5, 60_000L);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 10.0, "source", "test", "why");
        AtomicInteger deliveries = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<FactionMailbox.Result>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return mailbox.claim("player", claim.id(), ignored -> {
                        deliveries.incrementAndGet();
                        return true;
                    }, ignored -> true);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = 0;
            for (Future<FactionMailbox.Result> future : futures) {
                if (future.get(10, TimeUnit.SECONDS) == FactionMailbox.Result.SUCCESS) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, deliveries.get());
    }

    @Test
    void expiredClaimIsRefundedOnce() {
        MutableClock clock = new MutableClock();
        FactionMailbox mailbox = mailbox(clock, 5, 1_000L);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0, "source", "test", "why");
        clock.advance(Duration.ofSeconds(2));

        AtomicInteger refunds = new AtomicInteger();
        assertEquals(1, mailbox.expireStale(ignored -> {
            refunds.incrementAndGet();
            return true;
        }));
        assertEquals(FactionMailbox.Result.EXPIRED,
                mailbox.claim("player", claim.id(), ignored -> true, ignored -> true));
        assertEquals(1, refunds.get());
        assertEquals(0, mailbox.expireStale(ignored -> {
            refunds.incrementAndGet();
            return true;
        }));
        assertEquals(1, refunds.get());
    }

    @Test
    void capacityIsPerPlayer() {
        FactionMailbox mailbox = mailbox(new MutableClock(), 2, 60_000L);
        assertNotNull(mailbox.enqueue("p1", "player", 1.0, "source", "test", "one"));
        assertNotNull(mailbox.enqueue("p2", "player", 1.0, "source", "test", "two"));
        assertNull(mailbox.enqueue("p3", "player", 1.0, "source", "test", "three"));
        assertNotNull(mailbox.enqueue("p4", "other", 1.0, "source", "test", "other"));
    }

    @Test
    void retentionCompactsDeliveredAndRefundedExpiredClaimsOnly() {
        MutableClock clock = new MutableClock();
        FactionMailbox mailbox = mailbox(clock, 5, 1_000L);
        FactionMailbox.Claim delivered = mailbox.enqueue("delivered", "player", 5.0,
                "source", "test", "delivered");
        assertEquals(FactionMailbox.Result.SUCCESS,
                mailbox.claim("player", delivered.id(), ignored -> true, ignored -> true));

        FactionMailbox.Claim expired = mailbox.enqueue("expired", "player", 5.0,
                "source", "test", "expired");
        clock.advance(Duration.ofSeconds(2));
        assertEquals(1, mailbox.expireStale(ignored -> true));
        FactionMailbox.Claim pending = mailbox.enqueue("pending", "player", 5.0,
                "source", "test", "pending");

        clock.advance(Duration.ofDays(40));
        FactionMailbox.Claim recent = mailbox.enqueue("recent", "player", 5.0,
                "source", "test", "recent");
        assertEquals(FactionMailbox.Result.SUCCESS,
                mailbox.claim("player", recent.id(), ignored -> true, ignored -> true));

        assertEquals(2, mailbox.retain(clock.millis(), Duration.ofDays(30).toMillis()));
        assertNull(mailbox.get(delivered.id()));
        assertNull(mailbox.get(expired.id()));
        assertNotNull(mailbox.get(pending.id()), "pending work is never compacted");
        assertNotNull(mailbox.get(recent.id()), "recent delivered work is kept");
        assertTrue(mailbox.protectedKeys().contains("mailbox:" + pending.id()));
        assertFalse(mailbox.protectedKeys().contains("mailbox:" + delivered.id()));
        assertFalse(mailbox.protectedKeys().contains("mailbox:" + expired.id()));

        FactionMailbox replayed = mailbox(clock, 5, 1_000L);
        assertNull(replayed.get(delivered.id()));
        assertNull(replayed.get(expired.id()));
        assertNotNull(replayed.get(pending.id()));
        assertNotNull(replayed.get(recent.id()));
    }

    @Test
    void expiredClaimWithFailedRefundIsNotCompacted() {
        MutableClock clock = new MutableClock();
        FactionMailbox mailbox = mailbox(clock, 5, 1_000L);
        FactionMailbox.Claim claim = mailbox.enqueue("failed-refund", "player", 5.0,
                "source", "test", "failed refund");
        clock.advance(Duration.ofSeconds(2));
        assertEquals(1, mailbox.expireStale(ignored -> false));

        clock.advance(Duration.ofDays(40));
        assertEquals(0, mailbox.retain(clock.millis(), Duration.ofDays(30).toMillis()));
        assertNotNull(mailbox.get(claim.id()), "the pending refund must survive retention");
    }

    @Test
    void duplicateIdIsNotQueuedTwice() {
        FactionMailbox mailbox = mailbox(new MutableClock(), 5, 60_000L);
        FactionMailbox.Claim first = mailbox.enqueue("fixed", "player", 5.0, "source", "test", "one");
        FactionMailbox.Claim second = mailbox.enqueue("fixed", "player", 9.0, "source", "test", "two");
        assertEquals(first, second);
        assertEquals(1, mailbox.pendingFor("player").size());
    }

    @Test
    void crashAfterClaimKeyIsUnknownNotDelivered() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, 5, 60_000L, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0, "source", "test", "why");

        // Simulate the crash window: the key is claimed, the sink never ran.
        assertTrue(idempotency.claim("mailbox:" + claim.id()));
        FactionMailbox recovered = mailbox(clock, 5, 60_000L, idempotency());

        assertEquals(FactionMailbox.State.DELIVERY_UNKNOWN, recovered.get(claim.id()).state());
        assertEquals(1, recovered.unknownClaims().size());
        assertTrue(recovered.protectedKeys().contains("mailbox:" + claim.id()));
        assertTrue(recovered.pendingFor("player").isEmpty());
        AtomicInteger deliveries = new AtomicInteger();
        assertEquals(FactionMailbox.Result.DELIVERY_UNKNOWN,
                recovered.claim("player", claim.id(), ignored -> {
                    deliveries.incrementAndGet();
                    return true;
                }, ignored -> true));
        assertEquals(0, deliveries.get(), "an ambiguous claim must never be paid automatically");
    }

    @Test
    void lostIdempotencyKeyDoesNotMakeUnknownRetryable() throws Exception {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, 5, 60_000L, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0,
                "source", "test", "why");
        assertTrue(idempotency.claim("mailbox:" + claim.id()));

        FactionMailbox recovered = mailbox(clock, 5, 60_000L, idempotency());
        assertEquals(FactionMailbox.State.DELIVERY_UNKNOWN, recovered.get(claim.id()).state());
        assertTrue(Files.deleteIfExists(idempotency.file()));

        // The key is gone (lost/corrupt storage), but the claim stays ambiguous.
        FactionMailbox afterLoss = mailbox(clock, 5, 60_000L, idempotency());
        assertEquals(FactionMailbox.State.DELIVERY_UNKNOWN, afterLoss.get(claim.id()).state());
        assertTrue(afterLoss.pendingFor("player").isEmpty());
        AtomicInteger deliveries = new AtomicInteger();
        assertEquals(FactionMailbox.Result.DELIVERY_UNKNOWN,
                afterLoss.claim("player", claim.id(), ignored -> {
                    deliveries.incrementAndGet();
                    return true;
                }, ignored -> true));
        assertEquals(0, deliveries.get(), "a lost key must not make an ambiguous payout retryable");
    }

    @Test
    void throwingSinkIsUnknownAndKeepsTheKey() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, 5, 60_000L, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0, "source", "test", "why");

        assertEquals(FactionMailbox.Result.DELIVERY_UNKNOWN,
                mailbox.claim("player", claim.id(), ignored -> {
                    throw new IllegalStateException("provider timeout");
                }, ignored -> true));
        assertEquals(FactionMailbox.State.DELIVERY_UNKNOWN, mailbox.get(claim.id()).state());
        assertTrue(idempotency.isClaimed("mailbox:" + claim.id()));
    }

    @Test
    void reconcileUnknownAsPaidDoesNotCallTheSink() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, 5, 60_000L, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0, "source", "test", "why");
        assertTrue(idempotency.claim("mailbox:" + claim.id()));
        FactionMailbox recovered = mailbox(clock, 5, 60_000L, idempotency());

        assertTrue(recovered.resolveUnknown(claim.id(), true));
        assertEquals(FactionMailbox.State.DELIVERED, recovered.get(claim.id()).state());
        assertEquals(0, recovered.unknownClaims().size());
        AtomicInteger deliveries = new AtomicInteger();
        assertEquals(FactionMailbox.Result.ALREADY_CLAIMED,
                recovered.claim("player", claim.id(), ignored -> {
                    deliveries.incrementAndGet();
                    return true;
                }, ignored -> true));
        assertEquals(0, deliveries.get());
    }

    @Test
    void reconcileUnknownAsRetryReleasesTheKeyAndPaysOnce() {
        MutableClock clock = new MutableClock();
        IdempotencyStore idempotency = idempotency();
        FactionMailbox mailbox = mailbox(clock, 5, 60_000L, idempotency);
        FactionMailbox.Claim claim = mailbox.enqueue("p1", "player", 25.0, "source", "test", "why");
        assertTrue(idempotency.claim("mailbox:" + claim.id()));
        IdempotencyStore recoveredStore = idempotency();
        FactionMailbox recovered = mailbox(clock, 5, 60_000L, recoveredStore);

        assertTrue(recovered.resolveUnknown(claim.id(), false));
        assertFalse(recoveredStore.isClaimed("mailbox:" + claim.id()));
        assertEquals(FactionMailbox.State.PENDING, recovered.get(claim.id()).state());
        AtomicInteger deliveries = new AtomicInteger();
        assertEquals(FactionMailbox.Result.SUCCESS,
                recovered.claim("player", claim.id(), ignored -> {
                    deliveries.incrementAndGet();
                    return true;
                }, ignored -> true));
        assertEquals(1, deliveries.get());
    }

    private FactionMailbox mailbox(Clock clock, int capacity, long ttlMillis) {
        return mailbox(clock, capacity, ttlMillis, idempotency());
    }

    private FactionMailbox mailbox(Clock clock, int capacity, long ttlMillis,
                                   IdempotencyStore idempotency) {
        return new FactionMailbox(this.directory.resolve("mailbox.json"), idempotency, clock,
                capacity, ttlMillis, Logger.getLogger("test"));
    }

    private IdempotencyStore idempotency() {
        return new IdempotencyStore(this.directory.resolve("idempotency.json"),
                Logger.getLogger("test"));
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
