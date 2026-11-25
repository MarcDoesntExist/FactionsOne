package com.massivecraft.factions.shield;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers grace and shield window arithmetic.
 *
 * @author MarcDoesntExists
 */
class ShieldPolicyTest {

    private static final long HOUR = 3_600_000L;

    @Test
    void windowIsActiveUntilItsEnd() {
        assertTrue(ShieldPolicy.active(1_001L, 1_000L));
        assertFalse(ShieldPolicy.active(1_000L, 1_000L));
        assertFalse(ShieldPolicy.active(999L, 1_000L));
    }

    @Test
    void eitherWindowBlocks() {
        assertTrue(ShieldPolicy.blocks(2_000L, 0L, 1_000L));
        assertTrue(ShieldPolicy.blocks(0L, 2_000L, 1_000L));
        assertFalse(ShieldPolicy.blocks(0L, 1_000L, 1_000L));
        assertFalse(ShieldPolicy.blocks(0L, 0L, 1_000L));
    }

    @Test
    void hoursAreClamped() {
        assertEquals(1_000L + 2 * HOUR, ShieldPolicy.addHours(1_000L, 2));
        assertEquals(1_000L, ShieldPolicy.addHours(1_000L, -5));
        assertEquals(1_000L + 24L * 365L * HOUR, ShieldPolicy.addHours(1_000L, Integer.MAX_VALUE));
    }

    @Test
    void remainingMinutesRoundUp() {
        assertEquals(0L, ShieldPolicy.remainingMinutes(1_000L, 1_000L));
        assertEquals(1L, ShieldPolicy.remainingMinutes(1_001L, 1_000L));
        assertEquals(2L, ShieldPolicy.remainingMinutes(61_001L, 1_000L));
        assertEquals(60L, ShieldPolicy.remainingMinutes(1_000L + HOUR, 1_000L));
    }
}
