package com.massivecraft.factions;

import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Coordinates the small faction economy: points, upgrades, shop offers and
 * missions. All mutations validate the relevant FPerm before touching state.
 *
 * @author MarcDoesntExists
 */
public final class FeatureService {

    public enum Result {
        SUCCESS,
        NO_PERMISSION,
        NOT_FOUND,
        MAX_LEVEL,
        NOT_ENOUGH_POINTS,
        NO_SPACE,
        NOT_READY
    }

    private final Main plugin;
    private final FeatureConfig config;

    public FeatureService(Main plugin, FeatureConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public FeatureConfig getConfig() {
        return this.config;
    }

    private int bonus(Faction faction, FeatureConfig.UpgradeEffect effect) {
        if (faction == null) {
            return 0;
        }
        return UpgradeMath.bonus(this.config.upgrades(), effect, faction::getUpgradeLevel);
    }

    public int warpLimit(Faction faction) {
        return UpgradeMath.additive(Conf.warpMax,
                bonus(faction, FeatureConfig.UpgradeEffect.WARP_LIMIT));
    }

    public int tntLimit(Faction faction) {
        return UpgradeMath.additive(Conf.tntBankMax,
                bonus(faction, FeatureConfig.UpgradeEffect.TNT_LIMIT));
    }

    public int memberLimit(Faction faction) {
        return UpgradeMath.limit(Conf.factionMemberLimit,
                bonus(faction, FeatureConfig.UpgradeEffect.MEMBER_LIMIT), 0);
    }

    public int allyLimit(Faction faction) {
        return UpgradeMath.limit(Conf.factionAllyLimit,
                bonus(faction, FeatureConfig.UpgradeEffect.ALLY_LIMIT), 0);
    }

    public int claimLimit(Faction faction) {
        return UpgradeMath.limit(Conf.claimedLandsMax,
                bonus(faction, FeatureConfig.UpgradeEffect.CLAIM_LIMIT),
                Conf.claimedLandsMaxHardCap);
    }

    public double powerMax(Faction faction) {
        return UpgradeMath.power(Conf.powerPlayerMax,
                bonus(faction, FeatureConfig.UpgradeEffect.POWER_MAX));
    }

    public int upgradeCost(Faction faction, FeatureConfig.UpgradeDefinition definition) {
        int level = faction.getUpgradeLevel(definition.id());
        return UpgradeMath.cost(definition.baseCost(), definition.costMultiplier(), level);
    }

    public Result buyUpgrade(FPlayer fplayer, Faction faction, String id) {
        FeatureConfig.UpgradeDefinition definition = this.config.upgrade(id);
        if (definition == null) {
            return Result.NOT_FOUND;
        }
        if (!FPerm.UPGRADE.has(fplayer, faction, false)) {
            return Result.NO_PERMISSION;
        }
        int level = faction.getUpgradeLevel(definition.id());
        if (level >= definition.maxLevel()) {
            return Result.MAX_LEVEL;
        }
        int cost = this.upgradeCost(faction, definition);
        if (faction.getFactionPoints() < cost) {
            return Result.NOT_ENOUGH_POINTS;
        }

        faction.addFactionPoints(-cost);
        faction.setUpgradeLevel(definition.id(), level + 1);
        faction.addLog(fplayer.getName(), "UPGRADE", definition.id() + " -> " + (level + 1));
        return Result.SUCCESS;
    }

    public Result buyShopOffer(FPlayer fplayer, Faction faction, String id) {
        FeatureConfig.ShopOffer offer = this.config.shopOffer(id);
        if (offer == null) {
            return Result.NOT_FOUND;
        }
        if (!FPerm.SHOP.has(fplayer, faction, false)) {
            return Result.NO_PERMISSION;
        }
        Player player = fplayer.getPlayer();
        if (player == null) {
            return Result.NO_SPACE;
        }
        Material material = Material.matchMaterial(offer.material());
        if (material == null || material.isAir()) {
            return Result.NOT_FOUND;
        }
        int amount = Math.max(1, Math.min(64, offer.amount()));
        if (player.getInventory().firstEmpty() == -1) {
            return Result.NO_SPACE;
        }
        if (faction.getFactionPoints() < offer.cost()) {
            return Result.NOT_ENOUGH_POINTS;
        }

        faction.addFactionPoints(-offer.cost());
        player.getInventory().addItem(new ItemStack(material, amount));
        faction.addLog(fplayer.getName(), "SHOP", offer.id() + " x" + amount);
        return Result.SUCCESS;
    }

    public Result claimMission(FPlayer fplayer, Faction faction, String id) {
        FeatureConfig.MissionDefinition mission = this.config.mission(id);
        if (mission == null) {
            return Result.NOT_FOUND;
        }
        if (!FPerm.MISSIONS.has(fplayer, faction, false)) {
            return Result.NO_PERMISSION;
        }
        if (faction.isMissionCompleted(mission.id())) {
            return Result.NOT_READY;
        }
        if (faction.getMissionProgress(mission.id()) < mission.target()) {
            return Result.NOT_READY;
        }

        faction.markMissionCompleted(mission.id());
        faction.addFactionPoints(mission.rewardPoints());
        faction.addLog(fplayer.getName(), "MISSION", mission.id());
        return Result.SUCCESS;
    }

    public void progress(Faction faction, FeatureConfig.MissionType type, int amount) {
        for (FeatureConfig.MissionDefinition mission : this.config.missions()) {
            if (mission.type() != type || faction.isMissionCompleted(mission.id())) {
                continue;
            }
            int progress = Math.min(mission.target(),
                    faction.getMissionProgress(mission.id()) + Math.max(0, amount));
            faction.setMissionProgress(mission.id(), progress);
        }
    }

    public static String message(Result result) {
        return switch (result) {
            case SUCCESS -> Lang.featureSuccess;
            case NO_PERMISSION -> Lang.featureNoPermission;
            case NOT_FOUND -> Lang.featureNotFound;
            case MAX_LEVEL -> Lang.featureMaxLevel;
            case NOT_ENOUGH_POINTS -> Lang.featureNotEnoughPoints;
            case NO_SPACE -> Lang.featureNoSpace;
            case NOT_READY -> Lang.featureNotReady;
        };
    }

    public Main getPlugin() {
        return this.plugin;
    }
}
