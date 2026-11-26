package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.Lang;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdDemote extends FCommand {

    public CmdDemote() {
        super();
        this.aliases.add("demote");

        this.requiredArgs.add("player name");
        //this.optionalArgs.put("", "");

        this.permission = Permission.DEMOTE.node;
        this.disableOnLock = true;

        //To demote someone from member -> recruit you must be an officer.
        //To demote someone from officer -> member you must be a co-leader or leader.
        //To demote someone from co-leader -> officer you must be the leader.
        //We'll handle this internally
        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        FPlayer you = this.argAsBestFPlayerMatch(0);
        if (you == null)
            return;

        if (you.getFaction() != myFaction) {
            msg("%s<b> is not a member in your faction.", you.describeTo(fme, true));
            return;
        }

        if (you == fme) {
            msg("<b>The target player mustn't be yourself.");
            return;
        }

        if (you.getRole() == Rel.MEMBER) {
            if (!fme.getRole().isAtLeast(Rel.OFFICER)) {
                msg("<b>You must be an officer to demote a member to recruit.");
                return;
            }
            if (!you.setRole(Rel.RECRUIT)) {
                msg(Lang.roleChangeCancelled);
                return;
            }
            myFaction.addLog(fme.getName(), "ROLE", you.getName() + " -> " + Rel.RECRUIT);
            myFaction.msg("%s<i> was demoted to being a recruit in your faction.",
                you.describeTo(myFaction, true));
        } else if (you.getRole() == Rel.OFFICER) {
            if (!fme.getRole().isAtLeast(Rel.COLEADER)) {
                msg("<b>You must be a co-leader or the leader to demote an officer to member.");
                return;
            }
            if (!you.setRole(Rel.MEMBER)) {
                msg(Lang.roleChangeCancelled);
                return;
            }
            myFaction.addLog(fme.getName(), "ROLE", you.getName() + " -> " + Rel.MEMBER);
            myFaction.msg("%s<i> was demoted to being a member in your faction.",
                you.describeTo(myFaction, true));
        } else if (you.getRole() == Rel.COLEADER) {
            if (!fme.getRole().isAtLeast(Rel.LEADER)) {
                msg("<b>You must be the leader to demote a co-leader to officer.");
                return;
            }
            if (!you.setRole(Rel.OFFICER)) {
                msg(Lang.roleChangeCancelled);
                return;
            }
            myFaction.addLog(fme.getName(), "ROLE", you.getName() + " -> " + Rel.OFFICER);
            myFaction.msg("%s<i> was demoted to being an officer in your faction.",
                you.describeTo(myFaction, true));
        }
    }

}
