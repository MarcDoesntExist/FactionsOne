package com.massivecraft.factions;

import com.massivecraft.factions.util.LazyLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for warp password storage.
 *
 * @author MarcDoesntExists
 */
class FactionWarpTest {

    @Test
    void noPasswordAllowsEveryEntry() {
        FactionWarp warp = new FactionWarp("base", null, null);

        assertFalse(warp.hasPassword());
        assertTrue(warp.matches(null));
        assertTrue(warp.matches("anything"));
    }

    @Test
    void passwordIsHashedAndCompared() {
        FactionWarp warp = new FactionWarp("base", new LazyLocation("world", 0, 64, 0),
                FactionWarp.hash("secret"));

        assertTrue(warp.hasPassword());
        assertTrue(warp.matches("secret"));
        assertFalse(warp.matches("wrong"));
        assertFalse(warp.matches(null));
        assertFalse(warp.getPasswordHash().contains("secret"));
    }

    @Test
    void emptyPasswordsAreNotHashed() {
        assertNull(FactionWarp.hash(null));
        assertNull(FactionWarp.hash(""));
        assertNull(FactionWarp.hash("   "));
    }
}
