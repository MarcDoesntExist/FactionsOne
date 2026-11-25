package com.massivecraft.factions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persistent set of player IDs banned from a faction.
 *
 * @author MarcDoesntExists
 */
public class FactionBanList {

    private final Set<String> ids = new HashSet<String>();

    public boolean isBanned(String playerId) {
        return playerId != null && this.ids.contains(playerId);
    }

    public void ban(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            this.ids.add(playerId);
        }
    }

    public void unban(String playerId) {
        if (playerId != null) {
            this.ids.remove(playerId);
        }
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(this.ids);
    }

    public void sanitize() {
        this.ids.removeIf(id -> id == null || id.isBlank());
    }
}
