package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.struct.Rel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Headless coverage for the web map color policy.
 *
 * @author MarcDoesntExists
 */
class MapColorTest {

    @Test
    void safeAndWarZonesUseFixedColors() {
        assertEquals(0xF2C94C, MapColor.colorFor("-1", "SafeZone", Rel.NEUTRAL, false));
        assertEquals(0xEB5757, MapColor.colorFor("-2", "WarZone", Rel.ENEMY, false));
    }

    @Test
    void factionColorsAreStable() {
        int first = MapColor.colorFor("7", "Alpha", Rel.NEUTRAL, false);
        int second = MapColor.colorFor("7", "Alpha", Rel.NEUTRAL, false);

        assertEquals(first, second);
        assertNotEquals(0, first);
    }

    @Test
    void relationModeUsesFixedRelationColors() {
        assertEquals(0xFF5555, MapColor.colorFor("7", "Alpha", Rel.ENEMY, true));
        assertEquals(0x9C27B0, MapColor.colorFor("7", "Alpha", Rel.ALLY, true));
        assertEquals(0x55FF55, MapColor.colorFor("7", "Alpha", Rel.MEMBER, true));
    }

    @Test
    void relationModeFallsBackToFactionColorWithoutRelation() {
        int fallback = MapColor.colorFor("7", "Alpha", null, true);

        assertNotEquals(0xFF5555, fallback);
        assertEquals(MapColor.factionColor("Alpha"), fallback);
    }
}
