package com.massivecraft.factions.api;


import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.TerritoryAccess;

import java.util.*;

/**
 * API additions that are specific to FactionsOne.
 *
 * @author Daniel Saukel
 */
public class SkyAPI {

    /**
     * @return All of the claims of the faction.
     */
    public static Set<FLocation> getFactionClaims(Faction faction) {
        Set<FLocation> claims = new HashSet<>();
        Iterator<Map.Entry<FLocation, TerritoryAccess>> iter =
                Board.flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<FLocation, TerritoryAccess> entry = iter.next();
            if (entry.getValue().getHostFactionID().equals(faction.getId())) {
                claims.add(entry.getKey());
            }
        }
        return claims;
    }

    /**
     * Unclaims all of the land of the faction.
     */
    public static void unclaimAll(Faction faction) {
        Board.unclaimAll(faction.getId());
    }

    /**
     * @param uuid A player's UUID
     * @return The FPlayer that has this UUID
     */
    public static FPlayer getFPlayer(UUID uuid) {
        return FPlayers.i.get(uuid.toString());
    }

}
