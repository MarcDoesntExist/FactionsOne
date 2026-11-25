package com.massivecraft.factions;

import com.massivecraft.factions.struct.FFlag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for temporary faction shop perks.
 *
 * @author MarcDoesntExists
 */
class FactionPerksTest {

    @Test
    void powerBoostExpires() {
        FactionPerks perks = new FactionPerks();
        perks.setPower(2.5, 1_000L);
        assertEquals(2.5, perks.power(500L));
        assertEquals(0.0, perks.power(1_000L));
    }

    @Test
    void temporaryFlagExpires() {
        FactionPerks perks = new FactionPerks();
        perks.setFlag(FFlag.PVP, 1_000L);
        assertTrue(perks.flag(FFlag.PVP, 999L));
        assertFalse(perks.flag(FFlag.PVP, 1_000L));
    }

    @Test
    void sanitizeDropsExpiredEntries() {
        FactionPerks perks = new FactionPerks();
        perks.setPower(2.0, 100L);
        perks.setFlag(FFlag.PVP, 200L);
        perks.sanitize(150L);
        assertEquals(0.0, perks.power(150L));
        assertTrue(perks.flag(FFlag.PVP, 150L));
    }
}
