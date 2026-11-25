package com.massivecraft.factions.gui;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.rank.FactionRankEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only, paged faction leaderboard opened by {@code /f top menu}.
 *
 * @author MarcDoesntExists
 */
public final class TopMenu extends PagedMenu {

    public TopMenu(Player player, GuiConfig config, FactionRankEntry.Metric metric,
                   List<FactionRankEntry> entries) {
        super(player, config, config.title("menu.top.title", "<gold>Top factions"),
                config.rows("menu.top.rows", 6), ignored -> true);

        List<MenuItem> items = new ArrayList<MenuItem>();
        int rank = 1;
        for (FactionRankEntry entry : entries) {
            ItemStack icon = config.item("top.entry", player);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Main.main.txt.parse(
                        config.text("menu.top.entry-name", "<gold>#%s <white>%s"),
                        rank, entry.tag()));
                List<Component> lore = new ArrayList<Component>();
                for (String line : config.list("menu.top.entry-lore")) {
                    lore.add(Main.main.txt.parse(line, metric.key(), value(metric, entry)));
                }
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            items.add(new MenuItem(icon, null));
            rank++;
        }
        this.setEntries(items);
    }

    private static String value(FactionRankEntry.Metric metric, FactionRankEntry entry) {
        return switch (metric) {
            case BANK -> Econ.moneyString(entry.bank());
            case POWER -> String.format(java.util.Locale.ROOT, "%.1f", entry.power());
            case LAND -> String.valueOf(entry.land());
            case MEMBERS -> String.valueOf(entry.members());
            case POINTS -> String.valueOf(entry.points());
        };
    }
}
