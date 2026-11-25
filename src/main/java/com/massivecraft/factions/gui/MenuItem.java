package com.massivecraft.factions.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * One icon and its click action inside a menu.
 *
 * <p>{@code view} controls whether the icon is rendered; {@code click}
 * controls whether the action runs. Both are re-evaluated when the menu is
 * refreshed, so a permission or faction change cannot leave a stale action
 * usable.</p>
 *
 * @author MarcDoesntExists
 */
public record MenuItem(ItemStack icon, MenuRequirement requirement, Consumer<Player> action) {

    public MenuItem {
        icon = icon == null ? new ItemStack(org.bukkit.Material.BARRIER) : icon.clone();
        requirement = requirement == null ? MenuRequirement.allowAll() : requirement;
    }

    public MenuItem(ItemStack icon, Consumer<Player> action) {
        this(icon, MenuRequirement.allowAll(), action);
    }

    public MenuItem(ItemStack icon, Predicate<Player> view, Predicate<Player> click,
                    Consumer<Player> action) {
        this(icon, new MenuRequirement(view, click), action);
    }

    public boolean canView(Player player) {
        return this.requirement.canView(player);
    }

    public boolean canClick(Player player) {
        return this.requirement.canClick(player);
    }

    public void click(Player player) {
        if (this.requirement.canAct(player) && this.action != null) {
            this.action.accept(player);
        }
    }
}
