package com.massivecraft.factions.struct;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for territory access toggles and JSON persistence.
 *
 * @author MarcDoesntExists
 */
class TerritoryAccessTest {

    @Test
    void newAccessStartsAsDefault() {
        TerritoryAccess access = new TerritoryAccess("f1");

        assertEquals("f1", access.getHostFactionID());
        assertTrue(access.isHostFactionAllowed());
        assertTrue(access.isDefault());
        assertEquals("f1".hashCode(), access.hashCode());
    }

    @Test
    void hostFactionCanBeClosedAndReopened() {
        TerritoryAccess access = new TerritoryAccess("f1");

        assertFalse(access.toggleFaction("f1"));
        assertFalse(access.isHostFactionAllowed());
        assertFalse(access.isDefault());

        assertTrue(access.toggleFaction("f1"));
        assertTrue(access.isHostFactionAllowed());
        assertTrue(access.isDefault());
    }

    @Test
    void factionAndPlayerGrantsCanBeToggled() {
        TerritoryAccess access = new TerritoryAccess("f1");

        assertTrue(access.toggleFaction("f2"));
        assertTrue(access.factionHasAccess("f2"));
        assertFalse(access.isDefault());

        assertFalse(access.toggleFaction("f2"));
        assertFalse(access.factionHasAccess("f2"));
        assertTrue(access.isDefault());

        assertTrue(access.toggleFPlayer("player-uuid"));
        assertEquals("player-uuid", access.fplayerList());
        assertFalse(access.toggleFPlayer("player-uuid"));
        assertEquals("", access.fplayerList());
    }

    @Test
    void grantsCanBeAddedAndRemovedDirectly() {
        TerritoryAccess access = new TerritoryAccess("f1");

        access.addFaction("f2");
        access.addFPlayer("player-a");
        assertTrue(access.factionHasAccess("f2"));
        assertEquals("player-a", access.fplayerList());

        access.removeFaction("f2");
        access.removeFPlayer("player-a");
        assertFalse(access.factionHasAccess("f2"));
        assertEquals("", access.fplayerList());
    }

    @Test
    void hostMatchAcceptsFactionIdsOnly() {
        TerritoryAccess access = new TerritoryAccess("f1");

        assertTrue(access.doesHostFactionMatch("f1"));
        assertFalse(access.doesHostFactionMatch("f2"));
        assertFalse(access.doesHostFactionMatch(42));
        assertFalse(access.doesHostFactionMatch(null));
    }

    @Test
    void changingTheHostResetsGrants() {
        TerritoryAccess access = new TerritoryAccess("f1");
        access.setHostFactionAllowed(false);
        access.addFaction("f2");
        access.addFPlayer("player-a");

        access.setHostFactionID("f9");

        assertEquals("f9", access.getHostFactionID());
        assertTrue(access.isHostFactionAllowed());
        assertTrue(access.isDefault());
        assertFalse(access.factionHasAccess("f2"));
        assertEquals("", access.fplayerList());
    }

    @Test
    void defaultWildernessIsNotSerialized() {
        TerritoryAccess wilderness = new TerritoryAccess("0");

        assertNull(wilderness.serialize(wilderness, TerritoryAccess.class, null));
    }

    @Test
    void defaultFactionIsSerializedAsItsId() {
        TerritoryAccess access = new TerritoryAccess("f1");

        assertEquals("f1", access.serialize(access, TerritoryAccess.class, null).getAsString());
    }

    @Test
    void nonDefaultAccessSerializesAllFields() {
        TerritoryAccess access = new TerritoryAccess("f1");
        access.setHostFactionAllowed(false);
        access.addFaction("f2");
        access.addFPlayer("player-a");

        JsonObject json = access.serialize(access, TerritoryAccess.class, null).getAsJsonObject();

        assertEquals("f1", json.get("ID").getAsString());
        assertFalse(json.get("open").getAsBoolean());
        assertEquals("f2", json.getAsJsonArray("factions").get(0).getAsString());
        assertEquals("player-a", json.getAsJsonArray("fplayers").get(0).getAsString());
    }

    @Test
    void simpleJsonStringDeserializesToDefaultAccess() {
        TerritoryAccess parsed = new TerritoryAccess().deserialize(
                new JsonPrimitive("f1"), TerritoryAccess.class, null);

        assertEquals("f1", parsed.getHostFactionID());
        assertTrue(parsed.isHostFactionAllowed());
        assertTrue(parsed.isDefault());
    }

    @Test
    void objectJsonDeserializesAllFields() {
        JsonObject json = new JsonObject();
        json.addProperty("ID", "f1");
        json.addProperty("open", false);
        JsonArray factions = new JsonArray();
        factions.add("f2");
        json.add("factions", factions);
        JsonArray players = new JsonArray();
        players.add("player-a");
        json.add("fplayers", players);

        TerritoryAccess parsed = new TerritoryAccess().deserialize(
                json, TerritoryAccess.class, null);

        assertEquals("f1", parsed.getHostFactionID());
        assertFalse(parsed.isHostFactionAllowed());
        assertFalse(parsed.isDefault());
        assertTrue(parsed.factionHasAccess("f2"));
        assertEquals("player-a", parsed.fplayerList());
    }
}
