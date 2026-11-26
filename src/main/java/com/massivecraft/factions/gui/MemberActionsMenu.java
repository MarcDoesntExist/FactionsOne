package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;
import org.bukkit.entity.Player;

/**
 * Small action page for a member. The existing commands remain the authority
 * for role and FPerm checks; this page only offers actions the sender can use.
 *
 * @author MarcDoesntExists
 */
public final class MemberActionsMenu extends Menu {

    private final GuiConfig config;
    private final FPlayer target;

    public MemberActionsMenu(Player player, GuiConfig config, FPlayer target) {
        super(player, config.title("menu.member-actions.title", "<aqua>Member"),
            config.rows("menu.member-actions.rows", 3), ignored -> true);
        this.config = config;
        this.target = target;

        this.setItem(10, config.item("member-actions.promote", player),
            clicker -> this.command(clicker, "promote", Permission.PROMOTE.has(clicker)));
        this.setItem(12, config.item("member-actions.demote", player),
            clicker -> this.command(clicker, "demote", Permission.DEMOTE.has(clicker)));
        this.setItem(14, config.item("member-actions.kick", player),
            clicker -> this.command(clicker, "kick", FPerm.KICK.has(clicker, this.faction(), false)));
        this.setItem(16, config.item("member-actions.ban", player),
            clicker -> this.command(clicker, "ban", FPerm.BAN.has(clicker, this.faction(), false)));
        this.setItem(22, config.item("navigation.back", player),
            clicker -> new MembersMenu(clicker, this.config).open());
    }

    private com.massivecraft.factions.Faction faction() {
        return this.target.getFaction();
    }

    private void command(Player player, String command, boolean allowed) {
        if (!allowed) {
            player.sendMessage(this.config.title("menu.member-actions.denied",
                "<red>You cannot do that."));
            return;
        }
        player.closeInventory();
        player.performCommand("f " + command + " " + this.target.getName());
    }
}
