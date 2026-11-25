package com.massivecraft.factions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the faction ban list.
 *
 * @author MarcDoesntExists
 */
class FactionBanTest {

    @Test
    void banAndUnbanAreIdempotent() {
        FactionBanList bans = new FactionBanList();
        String id = "test-player-id";

        assertFalse(bans.isBanned(id));
        bans.ban(id);
        bans.ban(id);
        assertTrue(bans.isBanned(id));
        assertTrue(bans.getIds().contains(id));

        bans.unban(id);
        bans.unban(id);
        assertFalse(bans.isBanned(id));
    }

    @Test
    void blankIdsAreNotStored() {
        FactionBanList bans = new FactionBanList();

        bans.ban("");
        bans.ban("   ");
        bans.ban(null);

        assertTrue(bans.getIds().isEmpty());
    }
}
