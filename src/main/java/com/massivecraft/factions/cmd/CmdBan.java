package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import org.bukkit.Bukkit;

/**
 * Bans a player from joining or being invited to the sender's faction.
 *
 * @author MarcDoesntExists
 */
public class CmdBan extends FCommand {

    public CmdBan() {
        super();
        this.aliases.add("ban");

        this.requiredArgs.add("player");

        this.permission = Permission.BAN.node;
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

        if (target == fme) {
            msg("<b>You cannot ban yourself.");
            return;
        }

        if (!FPerm.BAN.has(fme, myFaction, true)) {
            return;
        }

        if (myFaction.isBanned(target)) {
            msg("<b>%s is already banned from your faction.", target.describeTo(fme, true));
            return;
        }

        if (target.getFaction() == myFaction) {
            if (target.getRole() == Rel.LEADER) {
                msg("<b>The leader cannot be banned.");
                return;
            }
            if (target.getRole() == Rel.COLEADER && fme.getRole() != Rel.LEADER
                && !fme.hasAdminMode()) {
                msg("<b>Only the faction leader can ban a co-leader.");
                return;
            }
            if (!Conf.canLeaveWithNegativePower && target.getPower() < 0) {
                msg("<b>You cannot ban that member until their power is positive.");
                return;
            }

            FPlayerLeaveEvent event = new FPlayerLeaveEvent(target, myFaction,
                FPlayerLeaveEvent.PlayerLeaveReason.KICKED);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }

            myFaction.deinvite(target);
            target.resetFactionData();
        }

        myFaction.ban(target);
        myFaction.addLog(fme.getName(), "BAN", target.getName());
        target.msg("%s<i> banned you from %s.", fme.describeTo(target, true),
            myFaction.describeTo(target));
        myFaction.msg("%s<i> banned %s<i> from joining the faction.", fme.describeTo(myFaction, true),
            target.describeTo(myFaction));

        if (Conf.logFactionKick) {
            Main.main.log(fme.getName() + " banned " + target.getName() + " from the faction: "
                + myFaction.getTagPlain());
        }
    }
}
