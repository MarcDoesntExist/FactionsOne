package com.massivecraft.factions.struct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the extended territory flags.
 *
 * @author MarcDoesntExists
 */
class FFlagTest {

    @Test
    void newFlagsHaveSafeDefaults() {
        assertTrue(FFlag.SPAWNERS.defaultDefaultValue);
        assertTrue(FFlag.TNT.defaultDefaultValue);
        assertTrue(FFlag.ANIMALS.defaultDefaultValue);
        assertTrue(FFlag.FIRE_FRIENDLY.defaultDefaultValue);
        assertFalse(FFlag.OPEN.defaultDefaultValue);
    }

    @Test
    void parseAcceptsNewNamesAndPrefixes() {
        assertEquals(FFlag.SPAWNERS, FFlag.parse("spawners"));
        assertEquals(FFlag.SPAWNERS, FFlag.parse("spa"));
        assertEquals(FFlag.TNT, FFlag.parse("tnt"));
        assertEquals(FFlag.ANIMALS, FFlag.parse("animals"));
        assertEquals(FFlag.FIRE_FRIENDLY, FFlag.parse("firefriendly"));
        assertEquals(FFlag.OPEN, FFlag.parse("open"));
        assertEquals(FFlag.OPEN, FFlag.parse("op"));
    }

    @Test
    void parseRejectsUnknownAndBlankValues() {
        assertNull(FFlag.parse(null));
        assertNull(FFlag.parse(""));
        assertNull(FFlag.parse("removed_flag"));
    }
}
