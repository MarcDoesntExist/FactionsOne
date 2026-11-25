package com.massivecraft.factions;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for the faction audit log view.
 *
 * @author MarcDoesntExists
 */
class FactionLogViewTest {

    @Test
    void filtersBankAndTaxEntriesOnly() {
        List<FactionLogEntry> entries = new ArrayList<FactionLogEntry>();
        entries.add(new FactionLogEntry(1L, "a", "BANK_IN", "10"));
        entries.add(new FactionLogEntry(2L, "b", "CLAIM", "0,0"));
        entries.add(new FactionLogEntry(3L, "tax", "TAX_PAID", "5"));
        entries.add(new FactionLogEntry(4L, "c", "BANK_OUT", "2"));
        entries.add(new FactionLogEntry(5L, "d", null, null));

        List<FactionLogEntry> filtered = FactionLogView.filter(entries, true);

        assertEquals(3, filtered.size());
        assertEquals("BANK_IN", filtered.get(0).getAction());
        assertEquals("TAX_PAID", filtered.get(1).getAction());
        assertEquals("BANK_OUT", filtered.get(2).getAction());
        assertFalse(FactionLogView.isBankEntry(entries.get(1)));
        assertTrue(FactionLogView.isBankEntry(entries.get(0)));
    }

    @Test
    void newestFirstReversesTheStoredOrder() {
        List<FactionLogEntry> entries = new ArrayList<FactionLogEntry>();
        entries.add(new FactionLogEntry(1L, "a", "A", "1"));
        entries.add(new FactionLogEntry(2L, "b", "B", "2"));
        entries.add(new FactionLogEntry(3L, "c", "C", "3"));

        List<FactionLogEntry> newest = FactionLogView.newestFirst(entries, false);

        assertEquals("C", newest.get(0).getAction());
        assertEquals("B", newest.get(1).getAction());
        assertEquals("A", newest.get(2).getAction());
    }

    @Test
    void rotationDropsTheOldestEntries() {
        List<FactionLogEntry> logs = new ArrayList<FactionLogEntry>();
        for (int i = 1; i <= 5; i++) {
            FactionLogView.addRotated(logs,
                    new FactionLogEntry(i, "actor", "ACTION_" + i, "detail"), 3);
        }

        assertEquals(3, logs.size());
        assertEquals("ACTION_3", logs.get(0).getAction());
        assertEquals("ACTION_4", logs.get(1).getAction());
        assertEquals("ACTION_5", logs.get(2).getAction());
    }
}
