package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
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
public class CmdOfficer extends FCommand {

    public CmdOfficer() {
        super();
        this.aliases.add("officer");

        this.requiredArgs.add("player name");
        //this.optionalArgs.put("", "");

        this.permission = Permission.OFFICER.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        FPlayer you = this.argAsBestFPlayerMatch(0);
        if (you == null)
            return;

        boolean permAny = Permission.OFFICER_ANY.has(sender, false);
        Faction targetFaction = you.getFaction();

        if (targetFaction != myFaction && !permAny) {
            msg("%s<b> is not a member in your faction.", you.describeTo(fme, true));
            return;
        }

        if (fme != null && fme.getRole().isLessThan(Rel.COLEADER) && !permAny) {
            msg("<b>You must be a co-leader or the faction leader.");
            return;
        }

        if (you == fme && !permAny) {
            msg("<b>The target player musn't be yourself.");
            return;
        }

        if (you.getRole() == Rel.LEADER) {
            msg("<b>The target player is a faction leader. Demote them first.");
            return;
        }

        if (you.getRole() == Rel.COLEADER) {
            if (fme != null && fme.getRole() != Rel.LEADER && !permAny) {
                msg("<b>Only the leader can change a co-leader's role.");
                return;
            }
            if (!you.setRole(Rel.OFFICER)) {
                msg(Lang.roleChangeCancelled);
                return;
            }
            targetFaction.msg("%s<i> is no longer co-leader in your faction.",
                you.describeTo(targetFaction, true));
            msg("<i>You have demoted %s<i> to officer.", you.describeTo(fme, true));
            return;
        }

        if (you.getRole() == Rel.OFFICER) {
            // Revoke
            if (!you.setRole(Rel.MEMBER)) {
                msg(Lang.roleChangeCancelled);
                return;
            }
            targetFaction.msg("%s<i> is no longer officer in your faction.",
                you.describeTo(targetFaction, true));
            msg("<i>You have removed officer status from %s<i>.", you.describeTo(fme, true));
        } else {
            // Give
            if (!you.setRole(Rel.OFFICER)) {
                msg(Lang.roleChangeCancelled);
                return;
            }
            targetFaction.msg("%s<i> was promoted to officer in your faction.",
                you.describeTo(targetFaction, true));
            msg("<i>You have promoted %s<i> to officer.", you.describeTo(fme, true));
        }
    }

}
