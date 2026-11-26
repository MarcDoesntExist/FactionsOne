package com.massivecraft.factions.value;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows a chunk key at most once per cooldown. Container automation can fire
 * many events per tick, so the claim value scan uses this to avoid rescanning
 * the same chunk on every hopper cycle.
 *
 * @author MarcDoesntExists
 */
public final class DirtyThrottle {

    private static final int MAX_KEYS = 4096;

    private final long cooldownMs;
    private final Map<String, Long> last = new HashMap<String, Long>();

    public DirtyThrottle(long cooldownMs) {
        this.cooldownMs = Math.max(0L, cooldownMs);
    }

    public boolean allow(String key, long now) {
        Long previous = this.last.get(key);
        if (previous != null && now - previous < this.cooldownMs) {
            return false;
        }
        if (previous == null && this.last.size() >= MAX_KEYS) {
            this.last.entrySet()
                .removeIf(entry -> now - entry.getValue() >= this.cooldownMs);
            if (this.last.size() >= MAX_KEYS) {
                return false;
            }
        }
        this.last.put(key, now);
        return true;
    }

    public void clear() {
        this.last.clear();
    }
}
