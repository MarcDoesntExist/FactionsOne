package com.massivecraft.factions.rank;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers ranking cache lifetime and invalidation.
 *
 * @author MarcDoesntExists
 */
class FactionRankingServiceTest {

    private static FactionRankEntry entry(String tag, double power) {
        return new FactionRankEntry(tag.toLowerCase(), tag, power, 0, 1, 0.0, 0, 0.0);
    }

    @Test
    void keepsTheSnapshotInsideTheTtlAndInvalidatesOnDemand() {
        TestClock clock = new TestClock();
        List<FactionRankEntry> rows = new ArrayList<FactionRankEntry>();
        rows.add(entry("Alpha", 1.0));
        AtomicInteger builds = new AtomicInteger();
        Supplier<List<FactionRankEntry>> provider = () -> {
            builds.incrementAndGet();
            return new ArrayList<FactionRankEntry>(rows);
        };
        FactionRankingService service = new FactionRankingService(clock, 30, provider);

        assertEquals("Alpha", service.top(FactionRankEntry.Metric.POWER, 5).get(0).tag());
        rows.add(entry("Bravo", 99.0));
        assertEquals(1, service.top(FactionRankEntry.Metric.POWER, 5).size());
        assertEquals(1, builds.get());

        clock.advance(30_000L);
        assertEquals("Bravo", service.top(FactionRankEntry.Metric.POWER, 5).get(0).tag());
        assertEquals(2, builds.get());

        rows.add(entry("Charlie", 50.0));
        service.invalidate();
        assertEquals(3, service.top(FactionRankEntry.Metric.POWER, 5).size());
        assertEquals(3, builds.get());
    }

    @Test
    void limitsAndEmptySnapshotsAreSafe() {
        TestClock clock = new TestClock();
        FactionRankingService service = new FactionRankingService(clock, 0,
                () -> List.of(entry("Alpha", 3.0), entry("Bravo", 2.0), entry("Charlie", 1.0)));

        assertEquals(2, service.top(FactionRankEntry.Metric.POWER, 2).size());
        assertEquals(3, service.top(FactionRankEntry.Metric.POWER, 999).size());

        FactionRankingService empty = new FactionRankingService(clock, 0, List::of);
        assertTrue(empty.top(FactionRankEntry.Metric.POWER, 10).isEmpty());
    }

    private static final class TestClock extends Clock {
        private long millis;
        private ZoneId zone = ZoneOffset.UTC;

        void advance(long amount) {
            this.millis += amount;
        }

        @Override
        public ZoneId getZone() {
            return this.zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            this.zone = zone;
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(this.millis);
        }
    }
}
