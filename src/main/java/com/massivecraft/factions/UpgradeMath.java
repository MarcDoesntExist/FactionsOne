package com.massivecraft.factions;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Pure arithmetic behind the configurable upgrade effects.
 *
 * @author MarcDoesntExists
 */
public final class UpgradeMath {

    private UpgradeMath() {
    }

    public static int bonus(List<FeatureConfig.UpgradeDefinition> definitions,
                            FeatureConfig.UpgradeEffect effect,
                            ToIntFunction<String> level) {
        if (definitions == null || effect == null || level == null) {
            return 0;
        }
        int bonus = 0;
        for (FeatureConfig.UpgradeDefinition definition : definitions) {
            if (definition == null || definition.effect() != effect) {
                continue;
            }
            int currentLevel = Math.max(0, level.applyAsInt(definition.id()));
            int amount = Math.max(0, definition.amountPerLevel());
            long addition = (long) currentLevel * amount;
            bonus = (int) Math.min(Integer.MAX_VALUE, (long) bonus + addition);
        }
        return bonus;
    }

    public static int additive(int base, int bonus) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, base) + Math.max(0, bonus));
    }

    /**
     * A base of zero means "no limit"; upgrades never turn it into a limit.
     */
    public static int limit(int base, int bonus, int hardCap) {
        if (base <= 0) {
            return 0;
        }
        long value = (long) base + Math.max(0, bonus);
        if (hardCap > 0) {
            value = Math.min(value, hardCap);
        }
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    public static double power(double base, int bonus) {
        return Math.max(0.0, base) + Math.max(0, bonus);
    }

    public static int cost(double baseCost, double multiplier, int level) {
        double cost = Math.max(0.0, baseCost)
                * Math.pow(Math.max(1.0, multiplier), Math.max(0, level));
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(cost));
    }
}
