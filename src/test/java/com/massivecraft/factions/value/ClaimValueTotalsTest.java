package com.massivecraft.factions.value;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Headless coverage for the claim value totals.
 *
 * @author MarcDoesntExists
 */
class ClaimValueTotalsTest {

    @Test
    void addsChunksToTheirFaction() {
        ClaimValueTotals totals = new ClaimValueTotals();
        totals.put("a", "one", 10.0);
        totals.put("b", "one", 5.0);
        totals.put("c", "two", 7.0);

        assertEquals(15.0, totals.value("one"));
        assertEquals(7.0, totals.value("two"));
        assertEquals(3, totals.chunkCount());
    }

    @Test
    void replacingAChunkSubtractsTheOldValue() {
        ClaimValueTotals totals = new ClaimValueTotals();
        totals.put("a", "one", 10.0);
        totals.put("a", "one", 4.0);

        assertEquals(4.0, totals.value("one"));
        assertEquals(1, totals.chunkCount());
    }

    @Test
    void reassigningAChunkMovesTheValue() {
        ClaimValueTotals totals = new ClaimValueTotals();
        totals.put("a", "one", 10.0);
        totals.put("a", "two", 6.0);

        assertEquals(0.0, totals.value("one"));
        assertEquals(6.0, totals.value("two"));
        assertEquals("two", totals.factionOf("a"));
    }

    @Test
    void removingAChunkSubtractsItsValue() {
        ClaimValueTotals totals = new ClaimValueTotals();
        totals.put("a", "one", 10.0);
        totals.put("b", "one", 5.0);
        totals.remove("a");

        assertEquals(5.0, totals.value("one"));
        assertEquals(1, totals.chunkCount());
    }

    @Test
    void removingAFactionClearsOnlyItsChunks() {
        ClaimValueTotals totals = new ClaimValueTotals();
        totals.put("a", "one", 10.0);
        totals.put("b", "one", 5.0);
        totals.put("c", "two", 7.0);

        totals.removeFaction("one");

        assertEquals(0.0, totals.value("one"));
        assertEquals(7.0, totals.value("two"));
        assertEquals(1, totals.chunkCount());
        assertNull(totals.factionOf("a"));
    }

    @Test
    void zeroTotalsAreNotRetainedAndNullIsSafe() {
        ClaimValueTotals totals = new ClaimValueTotals();
        totals.put("a", "one", 10.0);
        totals.remove("a");

        assertEquals(0.0, totals.value("one"));
        assertEquals(0.0, totals.value(null));
        assertEquals(0, totals.chunkCount());
    }
}
