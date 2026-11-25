package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Bukkit;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdJoin extends FCommand {
    public CmdJoin() {
        super();
        this.aliases.add("join");

        this.requiredArgs.add("faction");
        this.optionalArgs.put("player", "you");

        this.permission = Permission.JOIN.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        Faction faction = this.argAsFaction(0);
        if (faction == null)
            return;

        FPlayer fplayer = this.argAsBestFPlayerMatch(1, fme, false);
        if (faction.isBanned(fplayer)) {
            if (fplayer == fme) {
                msg("<b>You are banned from %s.", faction.getTag(fme));
            } else {
                msg("<b>%s is banned from %s.", fplayer.describeTo(fme, true), faction.getTag(fme));
            }
            return;
        }
        boolean samePlayer = fplayer == fme;

        if (!samePlayer && !Permission.JOIN_OTHERS.has(sender, false)) {
            msg("<b>You do not have permission to move other players into a faction.");
            return;
        }

        if (faction == fplayer.getFaction()) {
            msg("<b>%s %s already a member of %s", fplayer.describeTo(fme, true),
                    (samePlayer ? "are" : "is"), faction.getTag(fme));
            return;
        }

        int memberLimit = Main.main.featureService == null
                ? Conf.factionMemberLimit : Main.main.featureService.memberLimit(faction);
        if (memberLimit > 0 && faction.getFPlayers().size() >= memberLimit) {
            msg(Lang.memberLimit, faction.getTag(fme), memberLimit,
                    fplayer.describeTo(fme, false));
            return;
        }

        if (fplayer.hasFaction()) {
            msg("<b>%s must leave %s current faction first.", fplayer.describeTo(fme, true),
                    (samePlayer ? "your" : "their"));
            return;
        }

        if (!Conf.canLeaveWithNegativePower && fplayer.getPower() < 0) {
            msg("<b>%s cannot join a faction with a negative power level.",
                    fplayer.describeTo(fme, true));
            return;
        }

        if (!(faction.getOpen() || faction.isInvited(fplayer) || fme.hasAdminMode()
                || Permission.JOIN_ANY.has(sender, false))) {
            msg("<i>This faction requires invitation.");
            if (samePlayer)
                faction.msg("%s<i> tried to join your faction.", fplayer.describeTo(faction, true));
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make sure they can pay
        if (samePlayer && !canAffordCommand(Conf.econCostJoin, "to join a faction"))
            return;

        // trigger the join event (cancellable)
        FPlayerJoinEvent joinEvent = new FPlayerJoinEvent(FPlayers.i.get(me), faction,
                FPlayerJoinEvent.PlayerJoinReason.COMMAND);
        Bukkit.getServer().getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled())
            return;

        // then make 'em pay (if applicable)
        if (samePlayer && !payForCommand(Conf.econCostJoin, "to join a faction",
                "for joining a faction"))
            return;

        if (!samePlayer)
            fplayer.msg("<i>%s moved you into the faction %s.", fme.describeTo(fplayer, true),
                    faction.getTag(fplayer));
        faction.msg("<i>%s joined your faction.", fplayer.describeTo(faction, true));
        fme.msg("<i>%s successfully joined %s.", fplayer.describeTo(fme, true),
                faction.getTag(fme));

        fplayer.resetFactionData();
        fplayer.setFaction(faction);
        fplayer.setRole(
                Conf.factionRankDefault); // They have just joined a faction, start them out on the lowest rank (default config).

        faction.deinvite(fplayer);


        if (Conf.logFactionJoin) {
            if (samePlayer)
                Main.main.log("%s joined the faction %s.", fplayer.getName(), faction.getTagPlain());
            else
                Main.main.log("%s moved the player %s into the faction %s.", fme.getName(),
                        fplayer.getName(), faction.getTagPlain());
        }
    }
}
