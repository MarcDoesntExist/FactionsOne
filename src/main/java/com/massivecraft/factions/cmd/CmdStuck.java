package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

/**
 * Moves a stranded player to their faction home, the nearest own claim or the
 * world spawn after a warmup.
 *
 * @author MarcDoesntExists
 */
public class CmdStuck extends FCommand {

    public CmdStuck() {
        super();
        this.aliases.add("stuck");

        this.permission = Permission.STUCK.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = true;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        if (Main.main.stuckService == null) {
            msg(Lang.stuckUnavailable);
            return;
        }
        Main.main.stuckService.teleport(this.fme);
    }
}
