package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;

/**
 * Toggles faction-territory flight.
 *
 * @author MarcDoesntExists
 */
public final class CmdFly extends FCommand {

    public CmdFly() {
        super();
        this.aliases.add("fly");
        this.permission = Permission.FLY.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        Main.main.flyService.toggle(this.fme);
    }
}
