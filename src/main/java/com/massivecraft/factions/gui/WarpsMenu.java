package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionWarp;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists faction warps and teleports through the same service as /f warp.
 *
 * @author MarcDoesntExists
 */
public final class WarpsMenu extends PagedMenu {

    private final Faction faction;

    public WarpsMenu(Player player, GuiConfig config) {
        this(player, config, factionOf(player));
    }

    private WarpsMenu(Player player, GuiConfig config, Faction faction) {
        super(player, config, config.title("menu.warps.title", "<aqua>Warps"),
                config.rows("menu.warps.rows", 6),
                clicker -> {
                    FPlayer fplayer = FPlayers.i.get(clicker);
                    return fplayer != null && fplayer.getFaction() == faction
                            && FPerm.WARP.has(clicker, faction, false);
                });
        this.faction = faction;
        this.build();
    }

    private static Faction factionOf(Player player) {
        FPlayer fplayer = FPlayers.i.get(player);
        return fplayer == null ? null : fplayer.getFaction();
    }

    private void build() {
        List<MenuItem> entries = new ArrayList<MenuItem>();
        for (FactionWarp warp : this.faction.getWarps().values()) {
            ItemStack icon = this.config.item("main.warps", this.getPlayer());
            if (icon.getType() == Material.AIR) {
                icon = new ItemStack(Material.ENDER_PEARL);
            }
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Main.main.txt.parse("<aqua>%s", warp.getName()));
                List<Component> lore = new ArrayList<Component>();
                lore.add(Main.main.txt.parse("<gray>World: <white>%s",
                        warp.getLocation().getWorldName()));
                lore.add(Main.main.txt.parse(warp.hasPassword()
                        ? "<gray>Password required; use /f warp %s <password>"
                        : "<yellow>Click to teleport", warp.getName()));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            entries.add(new MenuItem(icon, clicker -> {
                if (warp.hasPassword()) {
                    clicker.sendMessage(Main.main.txt.parse(
                            "<b>Use /f warp %s <password>.", warp.getName()));
                    return;
                }
                Main.main.warpService.teleport(FPlayers.i.get(clicker), warp.getName(), null);
            }));
        }
        this.setEntries(entries);
    }
}
