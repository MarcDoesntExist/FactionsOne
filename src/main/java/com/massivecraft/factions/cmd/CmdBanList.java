package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;

/**
 * Lists players banned from the sender's faction.
 *
 * @author MarcDoesntExists
 */
public class CmdBanList extends FCommand {

    public CmdBanList() {
        super();
        this.aliases.add("banlist");

        this.permission = Permission.BAN_LIST.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = true;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        if (!FPerm.BAN.has(fme, myFaction, true)) {
            return;
        }

        if (myFaction.getBannedPlayerIds().isEmpty()) {
            msg("<i>Your faction has no banned players.");
            return;
        }

        msg("<i>Banned players:");
        for (String id : myFaction.getBannedPlayerIds()) {
            FPlayer banned = FPlayers.i.get(id);
            msg("<i>- %s", banned != null ? banned.getName() : id);
        }
    }
}
