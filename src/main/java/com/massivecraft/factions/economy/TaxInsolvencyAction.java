package com.massivecraft.factions.economy;

import java.util.Locale;

/**
 * Action applied once when a faction exhausts its tax grace periods.
 *
 * @author MarcDoesntExists
 */
public enum TaxInsolvencyAction {
    NONE,
    UNCLAIM;

    public static TaxInsolvencyAction parse(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
