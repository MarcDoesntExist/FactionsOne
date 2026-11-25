package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;

/**
 * Creates or replaces a faction warp.
 *
 * @author MarcDoesntExists
 */
public final class CmdSetWarp extends FCommand {

    public CmdSetWarp() {
        super();
        this.aliases.add("setwarp");
        this.requiredArgs.add("name");
        this.optionalArgs.put("password", "none");
        this.permission = Permission.SETWARP.node;
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        Main.main.warpService.setWarp(this.fme, this.argAsString(0), this.argAsString(1));
    }
}
