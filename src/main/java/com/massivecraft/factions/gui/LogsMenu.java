package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionLogEntry;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Read-only paged view over the faction audit log.
 *
 * @author MarcDoesntExists
 */
public final class LogsMenu extends PagedMenu {

    private final Faction faction;
    private final SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");

    public LogsMenu(Player player, GuiConfig config) {
        this(player, config, factionOf(player));
    }

    private LogsMenu(Player player, GuiConfig config, Faction faction) {
        super(player, config, config.title("menu.logs.title", "<gray>Faction logs"),
                config.rows("menu.logs.rows", 6),
                clicker -> {
                    FPlayer fplayer = FPlayers.i.get(clicker);
                    return fplayer != null && fplayer.getFaction() == faction
                            && FPerm.LOGS.has(clicker, faction, false);
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
        List<FactionLogEntry> logs = this.faction.getLogs();
        for (int i = logs.size() - 1; i >= 0; i--) {
            FactionLogEntry entry = logs.get(i);
            ItemStack icon = this.config.item("main.logs", this.getPlayer());
            if (icon.getType() == Material.AIR) {
                icon = new ItemStack(Material.BOOK);
            }
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Main.main.txt.parse("<white>%s <gray>%s", entry.getActor(),
                        entry.getAction()));
                List<Component> lore = new ArrayList<Component>();
                lore.add(Main.main.txt.parse("<gray>%s", this.format.format(
                        new Date(entry.getTimestamp()))));
                if (entry.getDetail() != null) {
                    lore.add(Main.main.txt.parse("<gray>%s", entry.getDetail()));
                }
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            entries.add(new MenuItem(icon, null));
        }
        this.setEntries(entries);
    }
}
