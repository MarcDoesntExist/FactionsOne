package com.massivecraft.factions.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory record of recently broken alliances. Pending windows are dropped
 * on restart on purpose: this is a gameplay cooldown, not durable data, so no
 * file format changes.
 *
 * @author MarcDoesntExists
 */
public final class BetrayalTracker {

    private final Map<String, Long> allianceBreaks = new ConcurrentHashMap<String, Long>();

    private static String key(String betrayerId, String victimId) {
        return betrayerId + "\u0000" + victimId;
    }

    public void recordBreak(String betrayerId, String victimId, long now, long windowMillis) {
        prune(now, windowMillis);
        this.allianceBreaks.put(key(betrayerId, victimId), now);
    }

    public boolean hasRecentBreak(String betrayerId, String victimId, long now,
                                  long windowMillis) {
        if (windowMillis <= 0L) {
            return false;
        }
        String key = key(betrayerId, victimId);
        Long brokenAt = this.allianceBreaks.get(key);
        if (brokenAt == null) {
            return false;
        }
        if (now - brokenAt > windowMillis) {
            this.allianceBreaks.remove(key, brokenAt);
            return false;
        }
        return true;
    }

    public void clear(String betrayerId, String victimId) {
        this.allianceBreaks.remove(key(betrayerId, victimId));
    }

    public int size() {
        return this.allianceBreaks.size();
    }

    private void prune(long now, long windowMillis) {
        long window = Math.max(0L, windowMillis);
        this.allianceBreaks.entrySet().removeIf(entry -> now - entry.getValue() > window);
    }
}
