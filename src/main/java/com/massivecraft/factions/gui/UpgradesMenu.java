package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FeatureConfig;
import com.massivecraft.factions.FeatureService;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.zcore.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists upgrades and buys the clicked one with faction points.
 *
 * @author MarcDoesntExists
 */
public final class UpgradesMenu extends PagedMenu {

    private final Faction faction;

    public UpgradesMenu(Player player, GuiConfig config) {
        this(player, config, factionOf(player));
    }

    private UpgradesMenu(Player player, GuiConfig config, Faction faction) {
        super(player, config, config.title("menu.upgrades.title", Lang.upgradeMenuTitle),
            config.rows("menu.upgrades.rows", 6),
            clicker -> {
                FPlayer fplayer = FPlayers.i.get(clicker);
                return fplayer != null && fplayer.getFaction() == faction
                    && FPerm.UPGRADE.has(clicker, faction, false);
            });
        this.faction = faction;
        this.build();
    }

    private static Faction factionOf(Player player) {
        FPlayer fplayer = FPlayers.i.get(player);
        return fplayer == null ? null : fplayer.getFaction();
    }

    private void build() {
        FeatureService service = Main.main.featureService;
        List<MenuItem> entries = new ArrayList<MenuItem>();
        for (FeatureConfig.UpgradeDefinition definition : service.getConfig().upgrades()) {
            ItemStack icon = this.config.item("main.upgrades", this.getPlayer());
            if (icon.getType() == Material.AIR) {
                icon = new ItemStack(Material.NETHER_STAR);
            }
            int level = this.faction.getUpgradeLevel(definition.id());
            int cost = service.upgradeCost(this.faction, definition);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Main.main.txt.parse(Lang.upgradeMenuName, definition.name()));
                List<Component> lore = new ArrayList<Component>();
                lore.add(Main.main.txt.parse(Lang.upgradeMenuLevel,
                    level, definition.maxLevel()));
                lore.add(Main.main.txt.parse(Lang.upgradeMenuCost, cost));
                lore.add(Main.main.txt.parse(Lang.upgradeMenuClick));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            entries.add(new MenuItem(icon, clicker -> {
                FPlayer fplayer = FPlayers.i.get(clicker);
                FeatureService.Result result = service.buyUpgrade(fplayer, this.faction,
                    definition.id());
                if (result == FeatureService.Result.SUCCESS) {
                    clicker.sendMessage(Main.main.txt.parse(Lang.upgradeMenuBought));
                    new UpgradesMenu(clicker, this.config).open();
                } else {
                    clicker.sendMessage(Main.main.txt.parse(FeatureService.message(result)));
                }
            }));
        }
        this.setEntries(entries);
    }
}
