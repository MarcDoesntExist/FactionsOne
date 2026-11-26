package com.massivecraft.factions;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the {@code MATERIAL:amount} entries used by mission rewards without
 * touching the Bukkit registry.
 *
 * @author MarcDoesntExists
 */
public final class MissionRewardParser {

    private MissionRewardParser() {
    }

    public static List<Reward> parse(List<String> specs) {
        List<Reward> result = new ArrayList<Reward>();
        if (specs == null) {
            return result;
        }
        for (String spec : specs) {
            if (spec == null || spec.isBlank()) {
                continue;
            }
            String[] parts = spec.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Mission reward must be MATERIAL:amount: " + spec);
            }
            String material = parts[0].trim();
            int amount = Integer.parseInt(parts[1].trim());
            if (material.isEmpty() || material.equalsIgnoreCase("AIR") || amount <= 0) {
                throw new IllegalArgumentException("Invalid mission reward item: " + spec);
            }
            result.add(new Reward(material, amount));
        }
        return result;
    }

    public record Reward(String material, int amount) {
    }
}
