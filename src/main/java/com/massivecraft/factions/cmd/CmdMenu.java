package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.gui.MainMenu;
import com.massivecraft.factions.struct.Permission;

/**
 * Opens the main faction menu.
 *
 * @author MarcDoesntExists
 */
public final class CmdMenu extends FCommand {

    public CmdMenu() {
        super();
        this.aliases.add("menu");
        this.aliases.add("gui");
        this.permission = Permission.MENU.node;
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = false;
        this.senderMustBeOfficer = false;
        this.senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        new MainMenu(this.fme.getPlayer(), Main.main.guiConfig).open();
    }
}
