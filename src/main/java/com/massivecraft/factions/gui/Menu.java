package com.massivecraft.factions.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Base class for all faction menus. The inventory holder is the menu itself, so
 * clicks can only be routed to the menu that created the view.
 *
 * @author MarcDoesntExists
 */
public abstract class Menu implements InventoryHolder {

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, MenuItem> items = new HashMap<>();
    private final Predicate<Player> allowed;

    protected Menu(Player player, Component title, int rows, Predicate<Player> allowed) {
        this.player = player;
        int size = Math.max(9, Math.min(54, rows * 9));
        this.inventory = Bukkit.createInventory(this, size, title);
        this.allowed = allowed == null ? ignored -> true : allowed;
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void setItem(int slot, ItemStack icon, Consumer<Player> action) {
        this.setItem(slot, new MenuItem(icon, action));
    }

    public void setItem(int slot, MenuItem item) {
        if (item == null) {
            this.items.remove(slot);
            this.inventory.setItem(slot, null);
            return;
        }
        this.items.put(slot, item);
        this.inventory.setItem(slot, item.canView(this.player) ? item.icon() : null);
    }

    /**
     * Re-evaluates item visibility after a faction, role or permission change.
     */
    public void refreshVisibility() {
        for (Map.Entry<Integer, MenuItem> entry : this.items.entrySet()) {
            MenuItem item = entry.getValue();
            this.inventory.setItem(entry.getKey(), item.canView(this.player) ? item.icon() : null);
        }
    }

    public void clearItems() {
        this.items.clear();
        this.inventory.clear();
    }

    public boolean isAllowed(Player player) {
        return player != null && player == this.player && this.allowed.test(player);
    }

    public void handleClick(int slot) {
        MenuItem item = this.items.get(slot);
        if (item != null && this.isAllowed(this.player) && item.canClick(this.player)) {
            item.click(this.player);
        }
    }

    public void open() {
        this.player.openInventory(this.inventory);
    }

    public void onClose() {
        // Subclasses can release resources here.
    }
}
