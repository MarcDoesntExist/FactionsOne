package com.massivecraft.factions.gui;

import org.bukkit.entity.Player;

import java.util.function.Predicate;

/**
 * Pure visibility and click requirements for one menu entry.
 *
 * <p>Both predicates are evaluated at click time. A hidden entry is never
 * actionable even if its old slot is still present in an open inventory.</p>
 *
 * @author MarcDoesntExists
 */
public record MenuRequirement(Predicate<Player> view, Predicate<Player> click) {

    public MenuRequirement {
        view = view == null ? ignored -> true : view;
        click = click == null ? ignored -> true : click;
    }

    public static MenuRequirement allowAll() {
        return new MenuRequirement(null, null);
    }

    public boolean canView(Player player) {
        return this.view.test(player);
    }

    public boolean canClick(Player player) {
        return this.click.test(player);
    }

    public boolean canAct(Player player) {
        return this.canView(player) && this.canClick(player);
    }
}
