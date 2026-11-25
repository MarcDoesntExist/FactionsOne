package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every FPerm with its current role matrix. A permitted user can open the
 * editor and toggle each role without typing /f perm.
 *
 * @author MarcDoesntExists
 */
public final class PermsMenu extends PagedMenu {

    private final Faction faction;

    public PermsMenu(Player player, GuiConfig config) {
        this(player, config, factionOf(player));
    }

    private PermsMenu(Player player, GuiConfig config, Faction faction) {
        super(player, config, config.title("menu.perms.title", "<gold>Permissions"),
                config.rows("menu.perms.rows", 6),
                clicker -> {
                    FPlayer fplayer = FPlayers.i.get(clicker);
                    return fplayer != null && fplayer.getFaction() == faction
                            && FPerm.MENU_PERMS.has(clicker, faction, false);
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
        for (FPerm perm : FPerm.values()) {
            entries.add(new MenuItem(this.icon(perm), clicker -> {
                if (this.faction == null || !FPerm.PERMS.has(clicker, this.faction, true)) {
                    return;
                }
                new PermEditorMenu(clicker, this.config, this.faction, perm).open();
            }));
        }
        this.setEntries(entries);
    }

    private ItemStack icon(FPerm perm) {
        ItemStack stack = this.config.item("perms.entry", this.getPlayer());
        if (stack.getType() == Material.AIR) {
            stack = new ItemStack(Material.PAPER);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Main.main.txt.parse(
                    this.config.text("menu.perms.entry-name", "<aqua>%s"), perm.getNicename()));
            List<Component> lore = new ArrayList<Component>();
            lore.add(Main.main.txt.parse(perm.getStateInfo(this.faction.getPermittedRelations(perm), true)));
            for (String line : this.config.list("menu.perms.entry-lore")) {
                lore.add(Main.main.txt.parse(line));
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
