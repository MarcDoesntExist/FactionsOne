package com.massivecraft.factions.gui;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Edits one FPerm across the nine relations. The existing /f perm command and
 * the faction model stay authoritative; this is only another way to call them.
 *
 * @author MarcDoesntExists
 */
public final class PermEditorMenu extends Menu {

    private final GuiConfig config;
    private final Faction faction;
    private final FPerm perm;

    public PermEditorMenu(Player player, GuiConfig config, Faction faction, FPerm perm) {
        super(player, Main.main.txt.parse(config.text("menu.perm-editor.title",
                        "<gold>Permissions: <white>%s"), perm.getNicename()),
                config.rows("menu.perm-editor.rows", 3),
                clicker -> {
                    com.massivecraft.factions.FPlayer fplayer = com.massivecraft.factions.FPlayers.i.get(clicker);
                    return fplayer != null && fplayer.getFaction() == faction
                            && FPerm.PERMS.has(clicker, faction, false);
                });
        this.config = config;
        this.faction = faction;
        this.perm = perm;
        this.render();
    }

    private void render() {
        this.clearItems();
        Set<Rel> permitted = this.faction.getPermittedRelations(this.perm);
        Rel[] relations = Rel.values();
        for (int slot = 0; slot < relations.length; slot++) {
            Rel rel = relations[slot];
            this.setItem(slot, this.roleIcon(rel, permitted.contains(rel)),
                    clicker -> this.toggle(clicker, rel));
        }
        this.setItem(22, this.config.item("navigation.back", this.getPlayer()),
                clicker -> new PermsMenu(clicker, this.config).open());
    }

    private ItemStack roleIcon(Rel rel, boolean enabled) {
        ItemStack stack = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Main.main.txt.parse(this.config.text("menu.perm-editor.role-name",
                    "<white>%s"), rel.name()));
            List<Component> lore = new ArrayList<Component>();
            for (String line : this.config.list("menu.perm-editor.role-lore")) {
                lore.add(Main.main.txt.parse(line, enabled ? "allowed" : "denied"));
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void toggle(Player player, Rel rel) {
        if (!FPerm.PERMS.has(player, this.faction, true)) {
            return;
        }
        boolean enabled = !this.faction.getPermittedRelations(this.perm).contains(rel);
        this.faction.setRelationPermitted(this.perm, rel, enabled);
        this.faction.addLog(player.getName(), "PERM",
                this.perm.name() + " " + rel.name() + "=" + enabled);

        // Keep the leader able to change permissions when the matrix is edited
        // through the GUI, matching /f perm's safeguard.
        if (this.perm == FPerm.PERMS && this.perm.getDefault().contains(Rel.LEADER)) {
            this.faction.setRelationPermitted(FPerm.PERMS, Rel.LEADER, true);
        }
        this.render();
        player.updateInventory();
    }
}
