package com.massivecraft.factions.value;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless coverage for the claim value dirty cooldown.
 *
 * @author MarcDoesntExists
 */
class DirtyThrottleTest {

    @Test
    void allowsOncePerCooldown() {
        DirtyThrottle throttle = new DirtyThrottle(1000L);

        assertTrue(throttle.allow("a", 0L));
        assertFalse(throttle.allow("a", 999L));
        assertTrue(throttle.allow("a", 1000L));
    }

    @Test
    void independentKeysDoNotBlockEachOther() {
        DirtyThrottle throttle = new DirtyThrottle(1000L);

        assertTrue(throttle.allow("a", 500L));
        assertTrue(throttle.allow("b", 500L));
        assertFalse(throttle.allow("b", 900L));
    }

    @Test
    void clearResetsTheCooldown() {
        DirtyThrottle throttle = new DirtyThrottle(1000L);

        assertTrue(throttle.allow("a", 0L));
        throttle.clear();
        assertTrue(throttle.allow("a", 1L));
    }
}
