package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.event.FactionRelationEvent;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.BetrayalRules;
import com.massivecraft.factions.util.BetrayalTracker;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Bukkit;

import java.util.Locale;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public abstract class FRelationCommand extends FCommand {
    public Rel targetRelation;

    public FRelationCommand() {
        super();
        this.requiredArgs.add("faction");
        //this.optionalArgs.put("", "");

        this.permission = Permission.RELATION.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = true;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        Faction them = this.argAsFaction(0);
        if (them == null)
            return;

        if (!them.isNormal()) {
            msg("<b>Nope! You can't...");
            return;
        }

        if (them == myFaction) {
            msg("<b>Nope! You can't declare a relation to yourself :)");
            return;
        }

        if (targetRelation == Rel.ENEMY && Main.main.shieldService != null
            && Main.main.shieldService.isProtected(myFaction)) {
            msg(Lang.shieldCannotAttack);
            return;
        }

        if (myFaction.getRelationWish(them) == targetRelation) {
            msg("<b>You already have that relation wish set with %s.", them.getTagPlain());
            return;
        }

        Rel oldRelation = myFaction.getRelationTo(them, true);
        long now = System.currentTimeMillis();
        long betrayalWindowMillis = Math.max(0L, Conf.betrayalWindowSeconds) * 1000L;
        BetrayalTracker betrayalTracker = Main.main.betrayalTracker;
        boolean recentBreak = betrayalTracker != null
            && betrayalTracker.hasRecentBreak(myFaction.getId(), them.getId(), now,
            betrayalWindowMillis);
        BetrayalRules.Outcome betrayalOutcome = BetrayalRules.classify(oldRelation,
            targetRelation, recentBreak, Conf.betrayalRulesEnabled);
        if (betrayalOutcome == BetrayalRules.Outcome.BLOCK_DIRECT_WAR) {
            msg(Lang.relationAllianceWar);
            return;
        }

        if (targetRelation == Rel.ALLY && myFaction.getRelationTo(them, true) != Rel.ALLY) {
            int limit = Main.main.featureService == null
                ? Conf.factionAllyLimit : Main.main.featureService.allyLimit(myFaction);
            if (limit > 0 && countAllies(myFaction) >= limit) {
                msg(Lang.allyLimit, limit);
                return;
            }
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(targetRelation.getRelationCost(), "to change a relation wish",
            "for changing a relation wish"))
            return;

        // try to set the new relation
        myFaction.setRelationWish(them, targetRelation);
        myFaction.addLog(fme.getName(), "RELATION", them.getTagPlain() + " " + targetRelation);
        Rel currentRelation = myFaction.getRelationTo(them, true);

        // if the relation change was successful
        if (targetRelation == currentRelation) {
            applyBetrayal(betrayalOutcome, myFaction, them, now, betrayalWindowMillis);
            // trigger the faction relation event
            FactionRelationEvent relationEvent =
                new FactionRelationEvent(myFaction, them, oldRelation, currentRelation);
            Bukkit.getServer().getPluginManager().callEvent(relationEvent);

            them.msg("%s<i> is now %s.", myFaction.describeTo(them, true),
                targetRelation.getDescFactionOne());
            myFaction.msg("%s<i> is now %s.", them.describeTo(myFaction, true),
                targetRelation.getDescFactionOne());
        }
        // inform the other faction of your request
        else {
            them.msg("%s<i> wishes to be %s.", myFaction.describeTo(them, true),
                targetRelation.getColor() + targetRelation.getDescFactionOne());
            them.msg("<i>Type <c>/" + Conf.baseCommandAliases.get(0) + " " + targetRelation + " "
                + myFaction.getTagPlain() + "<i> to accept.");
            myFaction.msg("%s<i> were informed that you wish to be %s<i>.",
                them.describeTo(myFaction, true),
                targetRelation.getColor() + targetRelation.getDescFactionOne());
        }

        // TODO: The ally case should work!!
        //   * this might have to be bumped up to make that happen, & allow ALLY,NEUTRAL only
        if (targetRelation != Rel.TRUCE && them.getFlag(FFlag.PEACEFUL)) {
            them.msg("<i>This will have no effect while your faction is peaceful.");
            myFaction.msg("<i>This will have no effect while their faction is peaceful.");
        }

        if (targetRelation != Rel.TRUCE && myFaction.getFlag(FFlag.PEACEFUL)) {
            them.msg("<i>This will have no effect while their faction is peaceful.");
            myFaction.msg("<i>This will have no effect while your faction is peaceful.");
        }
    }

    private void applyBetrayal(BetrayalRules.Outcome outcome, Faction faction, Faction other,
                               long now, long windowMillis) {
        BetrayalTracker tracker = Main.main.betrayalTracker;
        if (tracker == null || outcome == null) {
            return;
        }
        switch (outcome) {
            case RECORD_BREAK:
                tracker.recordBreak(faction.getId(), other.getId(), now, windowMillis);
                break;
            case CLEAR_BREAK:
                tracker.clear(faction.getId(), other.getId());
                break;
            case PENALIZE:
                if (!tracker.hasRecentBreak(faction.getId(), other.getId(), now, windowMillis)) {
                    return;
                }
                tracker.clear(faction.getId(), other.getId());
                applyPowerPenalty(faction, other);
                break;
            default:
                break;
        }
    }

    private void applyPowerPenalty(Faction traitor, Faction victim) {
        double configured = Math.max(0.0, Conf.betrayalPowerPenalty);
        double penalty = Math.min(configured, Math.max(0.0, traitor.getPower()));
        if (penalty <= 0.0) {
            return;
        }
        traitor.addPowerBoost(-penalty);
        String amount = String.format(Locale.ROOT, "%.1f", penalty);
        traitor.msg(Lang.relationBetrayalPenalty, amount);
        victim.msg(Lang.relationBetrayalVictim, traitor.getTagPlain());
        traitor.addLog(fme == null ? "server" : fme.getName(), "BETRAYAL",
            victim.getTagPlain() + " -" + amount);
    }

    private int countAllies(Faction faction) {
        int count = 0;
        for (Faction other : Factions.i.get()) {
            if (other != faction && faction.getRelationTo(other, true) == Rel.ALLY) {
                count++;
            }
        }
        return count;
    }
}
