package com.massivecraft.factions.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless coverage for the broken-alliance window.
 *
 * @author MarcDoesntExists
 */
class BetrayalTrackerTest {

    private static final long WINDOW = 10_000L;

    @Test
    void breakIsRecentInsideTheWindow() {
        BetrayalTracker tracker = new BetrayalTracker();
        tracker.recordBreak("a", "b", 1_000L, WINDOW);

        assertTrue(tracker.hasRecentBreak("a", "b", 5_000L, WINDOW));
    }

    @Test
    void breakExpiresAfterTheWindow() {
        BetrayalTracker tracker = new BetrayalTracker();
        tracker.recordBreak("a", "b", 1_000L, WINDOW);

        assertFalse(tracker.hasRecentBreak("a", "b", 12_000L, WINDOW));
        assertEquals(0, tracker.size());
    }

    @Test
    void pairsAreIsolatedAndDirectionMatters() {
        BetrayalTracker tracker = new BetrayalTracker();
        tracker.recordBreak("a", "b", 1_000L, WINDOW);

        assertFalse(tracker.hasRecentBreak("a", "c", 2_000L, WINDOW));
        assertFalse(tracker.hasRecentBreak("b", "a", 2_000L, WINDOW));
    }

    @Test
    void clearingRemovesTheWindow() {
        BetrayalTracker tracker = new BetrayalTracker();
        tracker.recordBreak("a", "b", 1_000L, WINDOW);
        tracker.clear("a", "b");

        assertFalse(tracker.hasRecentBreak("a", "b", 2_000L, WINDOW));
    }

    @Test
    void zeroWindowNeverCountsAsBetrayal() {
        BetrayalTracker tracker = new BetrayalTracker();
        tracker.recordBreak("a", "b", 1_000L, 0L);

        assertFalse(tracker.hasRecentBreak("a", "b", 1_000L, 0L));
    }

    @Test
    void recordingPrunesExpiredPairs() {
        BetrayalTracker tracker = new BetrayalTracker();
        tracker.recordBreak("a", "b", 1_000L, WINDOW);
        tracker.recordBreak("c", "d", 20_000L, WINDOW);

        assertEquals(1, tracker.size());
        assertTrue(tracker.hasRecentBreak("c", "d", 21_000L, WINDOW));
    }
}
