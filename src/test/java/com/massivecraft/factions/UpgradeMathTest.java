package com.massivecraft.factions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Headless checks for the upgrade effect arithmetic.
 *
 * @author MarcDoesntExists
 */
class UpgradeMathTest {

    private static FeatureConfig.UpgradeDefinition definition(
            String id, FeatureConfig.UpgradeEffect effect, int amountPerLevel) {
        return new FeatureConfig.UpgradeDefinition(id, id, 3, 5.0, 2.0, effect, amountPerLevel);
    }

    @Test
    void bonusSumsOnlyTheRequestedEffect() {
        List<FeatureConfig.UpgradeDefinition> definitions = List.of(
                definition("members", FeatureConfig.UpgradeEffect.MEMBER_LIMIT, 2),
                definition("members-extra", FeatureConfig.UpgradeEffect.MEMBER_LIMIT, 3),
                definition("allies", FeatureConfig.UpgradeEffect.ALLY_LIMIT, 4),
                definition("claims", FeatureConfig.UpgradeEffect.CLAIM_LIMIT, 6),
                definition("power", FeatureConfig.UpgradeEffect.POWER_MAX, 1),
                definition("warps", FeatureConfig.UpgradeEffect.WARP_LIMIT, 10));
        Map<String, Integer> levels = Map.of(
                "members", 2,
                "members-extra", 1,
                "allies", 1,
                "claims", 2,
                "power", 3,
                "warps", 4);

        assertEquals(7, UpgradeMath.bonus(definitions,
                FeatureConfig.UpgradeEffect.MEMBER_LIMIT, id -> levels.getOrDefault(id, 0)));
        assertEquals(4, UpgradeMath.bonus(definitions,
                FeatureConfig.UpgradeEffect.ALLY_LIMIT, id -> levels.getOrDefault(id, 0)));
        assertEquals(12, UpgradeMath.bonus(definitions,
                FeatureConfig.UpgradeEffect.CLAIM_LIMIT, id -> levels.getOrDefault(id, 0)));
        assertEquals(3, UpgradeMath.bonus(definitions,
                FeatureConfig.UpgradeEffect.POWER_MAX, id -> levels.getOrDefault(id, 0)));
    }

    @Test
    void limitKeepsTheBaseAndAppliesTheHardCap() {
        assertEquals(17, UpgradeMath.limit(10, 7, 0));
        assertEquals(15, UpgradeMath.limit(10, 7, 15));
        assertEquals(10, UpgradeMath.limit(10, -7, 0));
        assertEquals(0, UpgradeMath.limit(0, 7, 0), "zero base means no limit");
        assertEquals(5, UpgradeMath.additive(0, 5), "warp and TNT bases stay additive");
    }

    @Test
    void powerAndCostAreRoundedAndClamped() {
        assertEquals(13.0, UpgradeMath.power(10.0, 3));
        assertEquals(0.0, UpgradeMath.power(-1.0, 0));
        assertEquals(40, UpgradeMath.cost(5.0, 2.0, 3));
        assertEquals(2, UpgradeMath.cost(1.1, 1.0, 0));
        assertEquals(0, UpgradeMath.cost(0.0, 2.0, 5));
    }
}
