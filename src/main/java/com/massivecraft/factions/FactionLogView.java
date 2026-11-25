package com.massivecraft.factions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure view helpers for the faction audit log.
 *
 * @author MarcDoesntExists
 */
public final class FactionLogView {

    private FactionLogView() {
    }

    public static List<FactionLogEntry> filter(List<FactionLogEntry> entries, boolean bankOnly) {
        List<FactionLogEntry> result = new ArrayList<FactionLogEntry>();
        if (entries == null) {
            return result;
        }
        for (FactionLogEntry entry : entries) {
            if (entry != null && (!bankOnly || isBankEntry(entry))) {
                result.add(entry);
            }
        }
        return result;
    }

    public static List<FactionLogEntry> newestFirst(List<FactionLogEntry> entries, boolean bankOnly) {
        List<FactionLogEntry> result = filter(entries, bankOnly);
        Collections.reverse(result);
        return result;
    }

    public static void trim(List<FactionLogEntry> entries, int limit) {
        if (entries == null) {
            return;
        }
        int max = Math.max(1, limit);
        while (entries.size() > max) {
            entries.remove(0);
        }
    }

    public static void addRotated(List<FactionLogEntry> entries, FactionLogEntry entry, int limit) {
        if (entries == null || entry == null) {
            return;
        }
        entries.add(entry);
        trim(entries, limit);
    }

    public static boolean isBankEntry(FactionLogEntry entry) {
        if (entry == null || entry.getAction() == null) {
            return false;
        }
        String action = entry.getAction();
        return action.startsWith("BANK_") || action.startsWith("TAX_");
    }
}
