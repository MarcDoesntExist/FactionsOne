package com.massivecraft.factions.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.massivecraft.factions.struct.FFlag;

import java.lang.reflect.Type;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FFlagTypeAdapter implements JsonDeserializer<FFlag> {
    @Override
    public FFlag deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        if (json == null || json.isJsonNull() || !json.isJsonPrimitive()) {
            return null;
        }
        return FFlag.parse(json.getAsString());
    }
}
