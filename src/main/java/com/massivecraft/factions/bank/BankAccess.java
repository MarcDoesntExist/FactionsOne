package com.massivecraft.factions.bank;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.util.RelationUtil;

/**
 * Permission rules for moving money out of a faction bank.
 *
 * @author MarcDoesntExists
 */
public final class BankAccess {

    private BankAccess() {
    }

    /**
     * Mirrors the old economy rights check: system callers, members of the
     * same faction and holders of the bank withdraw permission may move a
     * faction's money.
     */
    public static boolean canControl(EconomyParticipator invoker, EconomyParticipator target) {
        Faction invokerFaction = RelationUtil.getFaction(invoker);

        if (invokerFaction == null) {
            return true;
        }
        if (invoker instanceof FPlayer && ((FPlayer) invoker).hasAdminMode()) {
            return true;
        }
        if (invoker == target) {
            return true;
        }

        Faction targetFaction = RelationUtil.getFaction(target);
        if (invoker == invokerFaction && invokerFaction == targetFaction) {
            return true;
        }
        if (target instanceof Faction && FPerm.BANK_WITHDRAW.has(invoker, targetFaction)) {
            return true;
        }

        invoker.msg("<h>%s<i> lacks permission to control <h>%s's<i> money.",
            invoker.describeTo(invoker, true), target.describeTo(invoker));
        return false;
    }
}
