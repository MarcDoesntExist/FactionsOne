package com.massivecraft.factions.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A menu whose content is split into pages with previous/next navigation.
 *
 * @author MarcDoesntExists
 */
public abstract class PagedMenu extends Menu {

    protected final GuiConfig config;
    private final int contentSize;
    private final List<MenuItem> entries = new ArrayList<MenuItem>();
    private int page;

    protected PagedMenu(Player player, GuiConfig config, Component title, int rows,
                        Predicate<Player> allowed) {
        super(player, title, rows, allowed);
        this.config = config;
        this.contentSize = Math.max(9, (Math.max(9, Math.min(54, rows * 9)) / 9 - 1) * 9);
    }

    protected void setEntries(List<MenuItem> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
        this.page = Math.max(0, Math.min(this.page, this.maxPage()));
        this.render();
    }

    protected int getContentSize() {
        return this.contentSize;
    }

    protected int getPage() {
        return this.page;
    }

    protected void render() {
        this.clearItems();
        int start = this.page * this.contentSize;
        int end = Math.min(this.entries.size(), start + this.contentSize);
        for (int i = start; i < end; i++) {
            this.setItem(i - start, this.entries.get(i));
        }

        int row = this.contentSize;
        ItemStack previous = this.config.item("navigation.previous", this.getPlayer());
        ItemStack next = this.config.item("navigation.next", this.getPlayer());
        ItemStack back = this.config.item("navigation.back", this.getPlayer());

        this.setItem(row, previous, player -> this.setPage(this.page - 1));
        this.setItem(row + 4, back, player -> new MainMenu(player, this.config).open());
        this.setItem(row + 8, next, player -> this.setPage(this.page + 1));
    }

    protected void setPage(int page) {
        int clamped = Math.max(0, Math.min(this.maxPage(), page));
        if (clamped != this.page) {
            this.page = clamped;
            this.render();
        }
    }

    protected int maxPage() {
        return this.contentSize == 0 ? 0 : Math.max(0,
                (this.entries.size() - 1) / this.contentSize);
    }

    protected static ItemStack icon(Material material) {
        return new ItemStack(material);
    }
}
