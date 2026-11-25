package com.massivecraft.factions.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * One icon and its click action inside a menu.
 *
 * @author MarcDoesntExists
 */
public record MenuItem(ItemStack icon, Consumer<Player> action) {

    public MenuItem {
        icon = icon == null ? new ItemStack(org.bukkit.Material.BARRIER) : icon.clone();
    }

    public void click(Player player) {
        if (this.action != null) {
            this.action.accept(player);
        }
    }
}
