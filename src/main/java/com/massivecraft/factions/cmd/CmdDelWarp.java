package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;

/**
 * Removes a faction warp.
 *
 * @author MarcDoesntExists
 */
public final class CmdDelWarp extends FCommand {

    public CmdDelWarp() {
        super();
        this.aliases.add("delwarp");
        this.aliases.add("removewarp");
        this.requiredArgs.add("name");
        this.permission = Permission.SETWARP.node;
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        Main.main.warpService.deleteWarp(this.fme, this.argAsString(0));
    }
}
