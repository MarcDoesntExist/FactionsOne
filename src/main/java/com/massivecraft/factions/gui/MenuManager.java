package com.massivecraft.factions.gui;

import com.massivecraft.factions.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Routes all interactions with menu inventories and blocks item movement.
 *
 * @author MarcDoesntExists
 */
public final class MenuManager implements Listener {

    private final Main plugin;
    private final GuiConfig config;

    public MenuManager(Main plugin, GuiConfig config) {
        this.plugin = plugin;
        this.config = config;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::closeUnauthorizedMenus, 20L, 20L);
    }

    private void closeUnauthorizedMenus() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu
                    && !menu.isAllowed(player)) {
                player.closeInventory();
            }
        }
    }

    public GuiConfig getConfig() {
        return this.config;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || player != menu.getPlayer()) {
            return;
        }
        if (!menu.isAllowed(player)) {
            player.closeInventory();
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        menu.handleClick(event.getSlot());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.onClose();
        }
    }
}
