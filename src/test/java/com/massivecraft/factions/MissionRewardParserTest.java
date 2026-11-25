package com.massivecraft.factions;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Headless checks for mission reward item parsing.
 *
 * @author MarcDoesntExists
 */
class MissionRewardParserTest {

    @Test
    void parsesMaterialAndAmount() {
        List<MissionRewardParser.Reward> items = MissionRewardParser.parse(
                List.of("DIAMOND:2", "DIRT:1"));
        assertEquals(2, items.size());
        assertEquals("DIAMOND", items.get(0).material());
        assertEquals(2, items.get(0).amount());
        assertEquals("DIRT", items.get(1).material());
        assertEquals(1, items.get(1).amount());
    }

    @Test
    void rejectsUnknownOrEmptyEntries() {
        assertThrows(IllegalArgumentException.class,
                () -> MissionRewardParser.parse(List.of("DIAMOND")));
        assertThrows(IllegalArgumentException.class,
                () -> MissionRewardParser.parse(List.of("DIAMOND:0")));
        assertThrows(IllegalArgumentException.class,
                () -> MissionRewardParser.parse(List.of("DIAMOND:abc")));
        assertThrows(IllegalArgumentException.class,
                () -> MissionRewardParser.parse(List.of("AIR:1")));
    }
}
