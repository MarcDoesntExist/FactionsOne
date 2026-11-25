package com.massivecraft.factions.adapters;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.massivecraft.factions.struct.FPerm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Headless tests for safe permission deserialization.
 *
 * @author MarcDoesntExists
 */
class FPermTypeAdapterTest {

    private final FPermTypeAdapter adapter = new FPermTypeAdapter();

    @Test
    void parsesKnownEnumNamesAndNicenames() {
        assertEquals(FPerm.BANK_DEPOSIT,
                adapter.deserialize(new JsonPrimitive("BANK_DEPOSIT"), FPerm.class, null));
        assertEquals(FPerm.WARP,
                adapter.deserialize(new JsonPrimitive("warp"), FPerm.class, null));
    }

    @Test
    void ignoresUnknownOrMalformedValues() {
        assertNull(adapter.deserialize(new JsonPrimitive("REMOVED_PERMISSION"), FPerm.class, null));
        assertNull(adapter.deserialize(new JsonPrimitive(""), FPerm.class, null));
        assertNull(adapter.deserialize(JsonNull.INSTANCE, FPerm.class, null));
        assertNull(adapter.deserialize(new JsonObject(), FPerm.class, null));
    }
}
