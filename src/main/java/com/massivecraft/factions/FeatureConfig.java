package com.massivecraft.factions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the small YAML registries used by warps, upgrades, shop and missions.
 *
 * @author MarcDoesntExists
 */
public final class FeatureConfig {

    public enum UpgradeEffect {
        WARP_LIMIT,
        TNT_LIMIT,
        MEMBER_LIMIT,
        ALLY_LIMIT,
        CLAIM_LIMIT,
        POWER_MAX
    }

    public record UpgradeDefinition(String id, String name, int maxLevel, double baseCost,
                                    double costMultiplier, UpgradeEffect effect,
                                    int amountPerLevel) {
    }

    public enum ShopOfferType {
        ITEM,
        PERK
    }

    public enum ShopPerk {
        POWER_BOOST,
        MONEY,
        POINTS,
        TEMP_FLAG
    }

    public record ShopOffer(String id, String material, int amount, int cost, String name,
                            List<String> lore, ShopOfferType type, ShopPerk perk,
                            double perkAmount, String perkFlag, int durationMinutes) {
    }

    public enum MissionType {
        CLAIM,
        TNT_DEPOSIT,
        BANK_DEPOSIT,
        KILL_PLAYER,
        KILL_MOB,
        MINE_BLOCK,
        PLAYER_KILLED
    }

    public enum MissionPeriod {
        NONE,
        DAILY,
        WEEKLY
    }

    public record MissionDefinition(String id, String name, String description, MissionType type,
                                    int target, int rewardPoints, double rewardMoney,
                                    List<String> rewardItems, MissionPeriod period) {
    }

    private final Main plugin;
    private final Map<String, UpgradeDefinition> upgrades = new LinkedHashMap<String, UpgradeDefinition>();
    private final Map<String, ShopOffer> shopOffers = new LinkedHashMap<String, ShopOffer>();
    private final Map<String, MissionDefinition> missions = new LinkedHashMap<String, MissionDefinition>();

    public FeatureConfig(Main plugin) {
        this.plugin = plugin;
        this.load();
    }

    private void load() {
        YamlConfiguration upgradesFile = this.loadFile("upgrades.yml");
        ConfigurationSection upgradeSection = upgradesFile.getConfigurationSection("upgrades");
        if (upgradeSection != null) {
            for (String id : upgradeSection.getKeys(false)) {
                ConfigurationSection section = upgradeSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                try {
                    UpgradeEffect effect = UpgradeEffect.valueOf(
                            section.getString("effect", "WARP_LIMIT").toUpperCase(Locale.ROOT));
                    this.upgrades.put(id.toLowerCase(Locale.ROOT), new UpgradeDefinition(
                            id.toLowerCase(Locale.ROOT),
                            section.getString("name", id),
                            Math.max(1, section.getInt("max-level", 1)),
                            Math.max(0.0, section.getDouble("base-cost", 0.0)),
                            Math.max(1.0, section.getDouble("cost-multiplier", 1.0)),
                            effect,
                            Math.max(0, section.getInt("amount-per-level", 0))));
                } catch (IllegalArgumentException exception) {
                    this.plugin.getLogger().warning("Invalid upgrade definition " + id + ": "
                            + exception.getMessage());
                }
            }
        }

        YamlConfiguration shopFile = this.loadFile("shop.yml");
        ConfigurationSection shopSection = shopFile.getConfigurationSection("offers");
        if (shopSection != null) {
            for (String id : shopSection.getKeys(false)) {
                ConfigurationSection section = shopSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                try {
                    ShopOfferType type = ShopOfferType.valueOf(
                            section.getString("type", "ITEM").toUpperCase(Locale.ROOT));
                    ShopPerk perk = type == ShopOfferType.PERK
                            ? ShopPerk.valueOf(
                                    section.getString("perk", "").toUpperCase(Locale.ROOT))
                            : null;
                    this.shopOffers.put(id.toLowerCase(Locale.ROOT), new ShopOffer(
                            id.toLowerCase(Locale.ROOT),
                            section.getString("material", "STONE"),
                            Math.max(1, section.getInt("amount", 1)),
                            Math.max(1, section.getInt("cost", 1)),
                            section.getString("name", id),
                            List.copyOf(section.getStringList("lore")),
                            type,
                            perk,
                            Math.max(0.0, section.getDouble("perk-amount",
                                    section.getDouble("amount", 0.0))),
                            section.getString("perk-flag", ""),
                            Math.max(0, section.getInt("duration-minutes", 0))));
                } catch (IllegalArgumentException exception) {
                    this.plugin.getLogger().warning("Invalid shop offer " + id + ": "
                            + exception.getMessage());
                }
            }
        }

        YamlConfiguration missionsFile = this.loadFile("missions.yml");
        ConfigurationSection missionSection = missionsFile.getConfigurationSection("missions");
        if (missionSection != null) {
            for (String id : missionSection.getKeys(false)) {
                ConfigurationSection section = missionSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                try {
                    MissionType type = MissionType.valueOf(
                            section.getString("type", "CLAIM").toUpperCase(Locale.ROOT));
                    MissionPeriod period = MissionPeriod.valueOf(
                            section.getString("period", "NONE").toUpperCase(Locale.ROOT));
                    this.missions.put(id.toLowerCase(Locale.ROOT), new MissionDefinition(
                            id.toLowerCase(Locale.ROOT),
                            section.getString("name", id),
                            section.getString("description", ""),
                            type,
                            Math.max(1, section.getInt("target", 1)),
                            Math.max(0, section.getInt("reward-points", 0)),
                            Math.max(0.0, section.getDouble("reward-money", 0.0)),
                            List.copyOf(section.getStringList("reward-items")),
                            period));
                } catch (IllegalArgumentException exception) {
                    this.plugin.getLogger().warning("Invalid mission definition: " + id);
                }
            }
        }
    }

    private YamlConfiguration loadFile(String name) {
        File file = new File(this.plugin.getDataFolder(), name);
        if (!file.exists()) {
            this.plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public UpgradeDefinition upgrade(String id) {
        return id == null ? null : this.upgrades.get(id.toLowerCase(Locale.ROOT));
    }

    public ShopOffer shopOffer(String id) {
        return id == null ? null : this.shopOffers.get(id.toLowerCase(Locale.ROOT));
    }

    public MissionDefinition mission(String id) {
        return id == null ? null : this.missions.get(id.toLowerCase(Locale.ROOT));
    }

    public List<UpgradeDefinition> upgrades() {
        return Collections.unmodifiableList(new ArrayList<UpgradeDefinition>(this.upgrades.values()));
    }

    public List<ShopOffer> shopOffers() {
        return Collections.unmodifiableList(new ArrayList<ShopOffer>(this.shopOffers.values()));
    }

    public List<MissionDefinition> missions() {
        return Collections.unmodifiableList(new ArrayList<MissionDefinition>(this.missions.values()));
    }
}
