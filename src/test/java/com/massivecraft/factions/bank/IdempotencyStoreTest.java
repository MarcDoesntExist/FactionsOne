package com.massivecraft.factions.bank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for the durable idempotency store.
 *
 * @author MarcDoesntExists
 */
class IdempotencyStoreTest {

    private static final int THREADS = 8;

    @TempDir
    Path directory;

    @Test
    void claimIsExclusiveAndSurvivesRestart() {
        Path file = this.directory.resolve("idempotency.json");
        IdempotencyStore store = new IdempotencyStore(file, Logger.getLogger("test"));
        assertTrue(store.claim("tax:a:1"));
        assertFalse(store.claim("tax:a:1"));

        IdempotencyStore replayed = new IdempotencyStore(file, Logger.getLogger("test"));
        assertTrue(replayed.isClaimed("tax:a:1"));
        assertEquals(1, replayed.size());
    }

    @Test
    void releaseAllowsANewClaim() {
        Path file = this.directory.resolve("idempotency.json");
        IdempotencyStore store = new IdempotencyStore(file, Logger.getLogger("test"));
        assertTrue(store.claim("payout:1"));
        store.release("payout:1");
        assertFalse(store.isClaimed("payout:1"));
        assertTrue(store.claim("payout:1"));
    }

    @Test
    void legacyKeyArrayIsMigrated() throws Exception {
        Path file = this.directory.resolve("idempotency.json");
        Files.writeString(file, "{\"keys\":[\"legacy\"]}", StandardCharsets.UTF_8);

        IdempotencyStore store = new IdempotencyStore(file, Logger.getLogger("test"));
        assertTrue(store.isClaimed("legacy"));
        assertEquals(1, store.size());
    }

    @Test
    void retentionRemovesOnlyClaimsOlderThanTheWindow() {
        Path file = this.directory.resolve("idempotency.json");
        IdempotencyStore store = new IdempotencyStore(file, Logger.getLogger("test"));
        assertTrue(store.claimAt("old", 1_000L));
        assertTrue(store.claimAt("new", 9_000L));

        assertEquals(1, store.retain(10_000L, 5_000L));
        assertFalse(store.isClaimed("old"));
        assertTrue(store.isClaimed("new"));
        assertEquals(1, new IdempotencyStore(file, Logger.getLogger("test")).size());
    }

    @Test
    void retentionKeepsClaimsCreatedInTheFuture() {
        IdempotencyStore store = new IdempotencyStore(
                this.directory.resolve("idempotency.json"), Logger.getLogger("test"));
        assertTrue(store.claimAt("future", 20_000L));
        assertEquals(0, store.retain(10_000L, 1_000L));
        assertTrue(store.isClaimed("future"));
    }

    @Test
    void retentionRespectsProtectedKeys() {
        IdempotencyStore store = new IdempotencyStore(
                this.directory.resolve("idempotency.json"), Logger.getLogger("test"));
        assertTrue(store.claimAt("keep", 1_000L));
        assertTrue(store.claimAt("drop", 1_000L));

        assertEquals(1, store.retain(10_000L, 0L, "keep"::equals));
        assertTrue(store.isClaimed("keep"));
        assertFalse(store.isClaimed("drop"));
    }

    @Test
    void concurrentClaimHasExactlyOneWinner() throws Exception {
        Path file = this.directory.resolve("idempotency.json");
        IdempotencyStore store = new IdempotencyStore(file, Logger.getLogger("test"));
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    if (store.claim("mailbox:one")) {
                        winners.incrementAndGet();
                    }
                    return null;
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, winners.get());
    }
}
