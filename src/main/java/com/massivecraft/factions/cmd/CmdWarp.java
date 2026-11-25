package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;

/**
 * Teleports a member to a faction warp.
 *
 * @author MarcDoesntExists
 */
public final class CmdWarp extends FCommand {

    public CmdWarp() {
        super();
        this.aliases.add("warp");
        this.requiredArgs.add("warp");
        this.optionalArgs.put("password", "none");
        this.permission = Permission.WARP.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        Main.main.warpService.teleport(this.fme, this.argAsString(0), this.argAsString(1));
    }
}
