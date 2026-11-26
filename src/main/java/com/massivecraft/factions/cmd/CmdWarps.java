package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FactionWarp;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;

/**
 * Lists the faction's warps; the actual teleport gate remains FPerm.WARP.
 *
 * @author MarcDoesntExists
 */
public final class CmdWarps extends FCommand {

    public CmdWarps() {
        super();
        this.aliases.add("warps");
        this.permission = Permission.WARP.node;
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        if (!FPerm.WARP.has(this.fme, this.myFaction, true)) {
            return;
        }
        if (this.myFaction.getWarps().isEmpty()) {
            msg("<i>Your faction has no warps.");
            return;
        }
        msg("<gold>Faction warps:");
        for (FactionWarp warp : this.myFaction.getWarps().values()) {
            msg("<aqua>%s <gray>(%s%s)", warp.getName(),
                warp.getLocation().getWorldName(),
                warp.hasPassword() ? ", password" : "");
        }
    }
}
