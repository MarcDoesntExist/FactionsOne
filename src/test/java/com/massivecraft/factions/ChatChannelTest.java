package com.massivecraft.factions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure recipient checks for faction and ally chat.
 *
 * @author MarcDoesntExists
 */
class ChatChannelTest {

    @Test
    void publicReachesEveryone() {
        assertTrue(ChatChannelResolver.accepts(ChatChannel.PUBLIC, null, null, null));
        assertTrue(ChatChannelResolver.accepts(ChatChannel.PUBLIC, "a", "b", "ENEMY"));
    }

    @Test
    void factionOnlyReachesSameFaction() {
        assertTrue(ChatChannelResolver.accepts(ChatChannel.FACTION, "a", "a", "MEMBER"));
        assertFalse(ChatChannelResolver.accepts(ChatChannel.FACTION, "a", "b", "ALLY"));
    }

    @Test
    void allyReachesOwnFactionAndDirectAllies() {
        assertTrue(ChatChannelResolver.accepts(ChatChannel.ALLY, "a", "a", "MEMBER"));
        assertTrue(ChatChannelResolver.accepts(ChatChannel.ALLY, "a", "b", "ALLY"));
        assertFalse(ChatChannelResolver.accepts(ChatChannel.ALLY, "a", "b", "TRUCE"));
        assertFalse(ChatChannelResolver.accepts(ChatChannel.ALLY, "a", "b", "NEUTRAL"));
        assertFalse(ChatChannelResolver.accepts(ChatChannel.ALLY, "a", "b", "ENEMY"));
    }
}
