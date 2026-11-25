package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.integration.Econ;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only faction summary opened from the main menu.
 *
 * @author MarcDoesntExists
 */
public final class FactionInfoMenu extends Menu {

    public FactionInfoMenu(Player player, GuiConfig config) {
        super(player, config.title("menu.info.title", "<gold>Faction info"),
                config.rows("menu.info.rows", 3), ignored -> true);
        FPlayer fplayer = FPlayers.i.get(player);
        Faction faction = fplayer == null ? null : fplayer.getFaction();
        if (faction == null) {
            this.setItem(22, config.item("navigation.back", player),
                    clicker -> new MainMenu(clicker, config).open());
            return;
        }

        ItemStack icon = config.item("info.card", player);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(Main.main.txt.parse(
                    config.text("menu.info.name", "<gold>%s"), faction.getTagPlain()));
            List<String> configured = config.list("menu.info.lore");
            String[] values = {
                    String.valueOf(faction.getDescription()),
                    String.valueOf(faction.getFPlayers().size()),
                    String.valueOf(faction.getFPlayersWhereOnline(true).size()),
                    faction.getPowerRounded() + "/" + faction.getPowerMaxRounded(),
                    String.valueOf(faction.getLandRounded()),
                    Econ.moneyString(Econ.getBalance(faction.getAccountId()))
            };
            List<Component> lore = new ArrayList<Component>();
            for (int i = 0; i < configured.size(); i++) {
                lore.add(Main.main.txt.parse(configured.get(i), i < values.length ? values[i] : ""));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        this.setItem(13, icon, null);
        this.setItem(22, config.item("navigation.back", player),
                clicker -> new MainMenu(clicker, config).open());
    }
}
