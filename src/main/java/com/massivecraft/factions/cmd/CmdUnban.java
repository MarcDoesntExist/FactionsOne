package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;

/**
 * Removes a player from the faction ban list.
 *
 * @author MarcDoesntExists
 */
public class CmdUnban extends FCommand {

    public CmdUnban() {
        super();
        this.aliases.add("unban");

        this.requiredArgs.add("player");

        this.permission = Permission.UNBAN.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = true;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        FPlayer target = this.argAsBestFPlayerMatch(0);
        if (target == null) {
            return;
        }

        if (!FPerm.BAN.has(fme, myFaction, true)) {
            return;
        }

        if (!myFaction.isBanned(target)) {
            msg("<b>%s is not banned from your faction.", target.describeTo(fme, true));
            return;
        }

        myFaction.unban(target);
        msg("<i>%s<i> is no longer banned from your faction.", target.describeTo(fme, true));
        target.msg("%s<i> unbanned you from %s.", fme.describeTo(target, true),
                myFaction.describeTo(target));
        myFaction.msg("%s<i> unbanned %s<i>.", fme.describeTo(myFaction, true),
                target.describeTo(myFaction));
    }
}
