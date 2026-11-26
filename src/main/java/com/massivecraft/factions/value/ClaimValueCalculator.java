package com.massivecraft.factions.value;

import java.util.Map;

/**
 * Turns one chunk's spawner, container and item counts into a value using the
 * configured per-type prices. Unknown or negative entries are ignored, so a
 * bad configuration cannot make a faction's value negative.
 *
 * @author MarcDoesntExists
 */
public final class ClaimValueCalculator {

    private ClaimValueCalculator() {
    }

    public static double valueOf(ClaimChunkData data,
                                 Map<String, Double> spawnerValues,
                                 Map<String, Double> containerValues,
                                 Map<String, Double> itemValues) {
        if (data == null) {
            return 0.0;
        }
        return sum(data.spawners(), spawnerValues)
            + sum(data.containers(), containerValues)
            + sum(data.items(), itemValues);
    }

    private static double sum(Map<String, Integer> counts, Map<String, Double> values) {
        if (counts.isEmpty() || values == null || values.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Double unit = values.get(entry.getKey());
            if (unit == null || unit.isNaN() || unit.isInfinite()) {
                continue;
            }
            total += Math.max(0.0, unit) * entry.getValue();
        }
        return total;
    }
}
