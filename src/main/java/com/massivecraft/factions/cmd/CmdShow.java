package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.shield.ShieldService;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.Lang;
import com.massivecraft.factions.zcore.util.TextUtil;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdShow extends FCommand {
    public CmdShow() {
        this.aliases.add("show");
        this.aliases.add("who");

        //this.requiredArgs.add("");
        this.optionalArgs.put("faction", "your");

        this.permission = Permission.SHOW.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        Faction faction = myFaction;
        if (this.argIsSet(0)) {
            faction = this.argAsFaction(0);
            if (faction == null)
                return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostShow, "to show faction information",
            "for showing faction information"))
            return;

        Collection<FPlayer> admins = faction.getFPlayersWhereRole(Rel.LEADER);
        Collection<FPlayer> coleaders = faction.getFPlayersWhereRole(Rel.COLEADER);
        Collection<FPlayer> mods = faction.getFPlayersWhereRole(Rel.OFFICER);
        Collection<FPlayer> normals = faction.getFPlayersWhereRole(Rel.MEMBER);
        Collection<FPlayer> recruits = faction.getFPlayersWhereRole(Rel.RECRUIT);

        msg(p.txt.titleize(faction.getTag(fme)));
        msg("<a>Description: <i>%s", faction.getDescription());

        // Display important flags
        // TODO: Find the non default flags, and display them instead.
        if (faction.getFlag(FFlag.PERMANENT)) {
            msg("<a>This faction is permanent - remaining even with no members.");
        }

        if (faction.getFlag(FFlag.PEACEFUL)) {
            msg("<a>This faction is peaceful - in truce with everyone.");
        }

        msg("<a>Joining: <i>" + (faction.getOpen() ?
            "no invitation is needed" :
            "invitation is required"));

        if (faction.isNormal()) {
            ShieldService shields = Main.main.shieldService;
            String shield;
            if (shields == null || !shields.isProtected(faction)) {
                shield = Lang.shieldInactive;
            } else {
                shield = String.format(Lang.shieldState,
                    shields.isGrace(faction) ? Lang.shieldGrace : Lang.shieldActive,
                    shields.remainingMinutes(faction));
            }
            String raid = faction.hasLandInflation() ? Lang.showRaidable : Lang.showSafe;
            String war = faction.getRelationTo(fme) == Rel.ENEMY
                ? Lang.showAtWar : Lang.showNotAtWar;
            msg(Lang.showState, shield, raid, war);
        }

        double powerBoost = faction.getPowerBoost();
        String boost = (powerBoost == 0.0) ?
            "" :
            (powerBoost > 0.0 ? " (bonus: " : " (penalty: ") + powerBoost + ")";
        msg("<a>Land / Power / Maxpower: <i> %d/%d/%d %s", faction.getLandRounded(),
            faction.getPowerRounded(), faction.getPowerMaxRounded(), boost);

        // show the land value
        if (Econ.shouldBeUsed()) {
            double value = Econ.calculateTotalLandValue(faction.getLandRounded());
            double refund = value * Conf.econClaimRefundMultiplier;
            if (value > 0) {
                String stringValue = Econ.moneyString(value);
                String stringRefund =
                    (refund > 0.0) ? (" (" + Econ.moneyString(refund) + " depreciated)") : "";
                msg("<a>Total land value: <i>" + stringValue + stringRefund);
            }

            //Show bank contents
            if (Conf.bankEnabled) {
                msg("<a>Bank contains: <i>" + Econ
                    .moneyString(Econ.getBalance(faction.getAccountId())));
            }
        }

        Component sepparator = Component.text(", ", p.txt.getTagStyle("i"));

        // List the relations to other factions
        Map<Rel, List<Component>> relationTags = faction.getFactionTagsPerRelation(fme, true);

        if (faction.getFlag(FFlag.PEACEFUL)) {
            sendMessage(p.txt.parse("<a>In Truce with:<i> *everyone*"));
        } else {
            sendMessage(p.txt.parse("<a>In Truce with: ").append(TextUtil
                .implodeComponents(relationTags.get(Rel.TRUCE), sepparator)));
        }

        sendMessage(p.txt.parse("<a>Allied to: ").append(TextUtil
            .implodeComponents(relationTags.get(Rel.ALLY), sepparator)));
        sendMessage(p.txt.parse("<a>Enemies: ").append(TextUtil
            .implodeComponents(relationTags.get(Rel.ENEMY), sepparator)));

        // List the members...
        List<Component> memberOnlineNames = new ArrayList<Component>();
        List<Component> memberOfflineNames = new ArrayList<Component>();

        for (FPlayer follower : admins) {
            if (follower.isOnlineAndVisibleTo(me)) {
                memberOnlineNames.add(follower.getNameAndTitle(fme));
            } else {
                memberOfflineNames.add(follower.getNameAndTitle(fme));
            }
        }

        for (FPlayer follower : coleaders) {
            if (follower.isOnlineAndVisibleTo(me)) {
                memberOnlineNames.add(follower.getNameAndTitle(fme));
            } else {
                memberOfflineNames.add(follower.getNameAndTitle(fme));
            }
        }

        for (FPlayer follower : mods) {
            if (follower.isOnlineAndVisibleTo(me)) {
                memberOnlineNames.add(follower.getNameAndTitle(fme));
            } else {
                memberOfflineNames.add(follower.getNameAndTitle(fme));
            }
        }

        for (FPlayer follower : normals) {
            if (follower.isOnlineAndVisibleTo(me)) {
                memberOnlineNames.add(follower.getNameAndTitle(fme));
            } else {
                memberOfflineNames.add(follower.getNameAndTitle(fme));
            }
        }

        for (FPlayer follower : recruits) {
            if (follower.isOnline()) {
                memberOnlineNames.add(follower.getNameAndTitle(fme));
            } else {
                memberOfflineNames.add(follower.getNameAndTitle(fme));
            }
        }
        sendMessage(
            p.txt.parse("<a>Members online: ")
                .append(TextUtil.implodeComponents(memberOnlineNames, sepparator)));
        sendMessage(
            p.txt.parse("<a>Members offline: ")
                .append(TextUtil.implodeComponents(memberOfflineNames, sepparator)));
    }

}
