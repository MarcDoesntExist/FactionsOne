package com.massivecraft.factions.rank;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers ranking snapshots and metric ordering.
 *
 * @author MarcDoesntExists
 */
class FactionRankEntryTest {

    private static FactionRankEntry entry(String tag, double power, int land) {
        return new FactionRankEntry(tag.toLowerCase(), tag, power, land, 1, 0.0, 0, 0.0);
    }

    @Test
    void parsesMetricsIgnoringCase() {
        assertEquals(FactionRankEntry.Metric.POWER, FactionRankEntry.Metric.parse(" Power "));
        assertEquals(FactionRankEntry.Metric.BANK, FactionRankEntry.Metric.parse("BANK"));
        assertNull(FactionRankEntry.Metric.parse("unknown"));
        assertNull(FactionRankEntry.Metric.parse(null));
    }

    @Test
    void sortsDescendingAndBreaksTiesByTag() {
        List<FactionRankEntry> rows = new ArrayList<FactionRankEntry>();
        rows.add(entry("Charlie", 10.0, 4));
        rows.add(entry("Alpha", 10.0, 2));
        rows.add(entry("Bravo", 20.0, 1));
        rows.sort(FactionRankEntry.order(FactionRankEntry.Metric.POWER));

        assertEquals("Bravo", rows.get(0).tag());
        assertEquals("Alpha", rows.get(1).tag());
        assertEquals("Charlie", rows.get(2).tag());

        rows.sort(FactionRankEntry.order(FactionRankEntry.Metric.LAND));
        assertEquals("Charlie", rows.get(0).tag());
        assertEquals("Alpha", rows.get(1).tag());
        assertEquals("Bravo", rows.get(2).tag());
    }

    @Test
    void metricValuesMatchTheirColumn() {
        FactionRankEntry row = new FactionRankEntry("a", "Alpha", 12.5, 7, 3, 40.0, 9, 250.0);
        assertEquals(12.5, FactionRankEntry.Metric.POWER.value(row));
        assertEquals(7.0, FactionRankEntry.Metric.LAND.value(row));
        assertEquals(3.0, FactionRankEntry.Metric.MEMBERS.value(row));
        assertEquals(40.0, FactionRankEntry.Metric.BANK.value(row));
        assertEquals(9.0, FactionRankEntry.Metric.POINTS.value(row));
        assertEquals(250.0, FactionRankEntry.Metric.VALUE.value(row));
        assertEquals(FactionRankEntry.Metric.VALUE, FactionRankEntry.Metric.parse("value"));
    }
}
