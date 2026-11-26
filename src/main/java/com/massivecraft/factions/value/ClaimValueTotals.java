package com.massivecraft.factions.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the per-chunk and per-faction claim value totals. Pure so the
 * add/replace/remove arithmetic is unit tested without a server.
 *
 * @author MarcDoesntExists
 */
public final class ClaimValueTotals {

    private static final double EPSILON = 0.000001;

    private final Map<String, Double> chunkValues = new HashMap<String, Double>();
    private final Map<String, String> chunkFactions = new HashMap<String, String>();
    private final Map<String, Double> factionValues = new HashMap<String, Double>();

    public void put(String chunkKey, String factionId, double value) {
        String oldFaction = this.chunkFactions.put(chunkKey, factionId);
        Double oldValue = this.chunkValues.put(chunkKey, value);
        if (oldFaction != null && oldValue != null) {
            add(oldFaction, -oldValue);
        }
        add(factionId, value);
    }

    public void remove(String chunkKey) {
        String factionId = this.chunkFactions.remove(chunkKey);
        Double value = this.chunkValues.remove(chunkKey);
        if (factionId != null && value != null) {
            add(factionId, -value);
        }
    }

    public void removeFaction(String factionId) {
        if (factionId == null) {
            return;
        }
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, String> entry : this.chunkFactions.entrySet()) {
            if (factionId.equals(entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        for (String key : keys) {
            remove(key);
        }
        this.factionValues.remove(factionId);
    }

    public double value(String factionId) {
        if (factionId == null) {
            return 0.0;
        }
        return this.factionValues.getOrDefault(factionId, 0.0);
    }

    public int chunkCount() {
        return this.chunkValues.size();
    }

    public String factionOf(String chunkKey) {
        return this.chunkFactions.get(chunkKey);
    }

    public void clear() {
        this.chunkValues.clear();
        this.chunkFactions.clear();
        this.factionValues.clear();
    }

    private void add(String factionId, double delta) {
        double next = this.factionValues.getOrDefault(factionId, 0.0) + delta;
        if (next <= EPSILON) {
            this.factionValues.remove(factionId);
        } else {
            this.factionValues.put(factionId, next);
        }
    }
}
