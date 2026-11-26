package com.massivecraft.factions.gui;

import org.bukkit.entity.Player;

/**
 * Read-only page used for hub entries whose feature belongs to a later phase.
 *
 * @author MarcDoesntExists
 */
public final class InfoMenu extends Menu {

    public InfoMenu(Player player, GuiConfig config, String key) {
        super(player, config.title("menu." + key + ".title", key),
            config.rows("menu." + key + ".rows", 3), ignored -> true);
        this.setItem(13, config.item("main." + key, player), null);
        this.setItem(22, config.item("navigation.back", player),
            clicker -> new MainMenu(clicker, config).open());
    }
}
