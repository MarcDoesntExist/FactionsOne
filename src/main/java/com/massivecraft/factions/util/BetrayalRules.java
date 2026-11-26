package com.massivecraft.factions.util;

import com.massivecraft.factions.struct.Rel;

/**
 * Decides what a relation change means for the betrayal rules. The command
 * layer only applies the returned outcome, which keeps the policy headless and
 * testable.
 *
 * @author MarcDoesntExists
 */
public final class BetrayalRules {

    public enum Outcome {
        NONE,
        BLOCK_DIRECT_WAR,
        RECORD_BREAK,
        CLEAR_BREAK,
        PENALIZE
    }

    private BetrayalRules() {
    }

    /**
     * @param currentRelation relation before the change, ignoring peaceful
     * @param targetRelation  relation the faction is asking for
     * @param recentBreak     whether this faction broke an alliance with the
     *                        target inside the configured window
     * @param enabled         whether the betrayal rules are active
     */
    public static Outcome classify(Rel currentRelation, Rel targetRelation,
                                   boolean recentBreak, boolean enabled) {
        if (!enabled || currentRelation == null || targetRelation == null) {
            return Outcome.NONE;
        }
        if (targetRelation == Rel.ALLY) {
            return recentBreak ? Outcome.CLEAR_BREAK : Outcome.NONE;
        }
        if (currentRelation == Rel.ALLY && targetRelation == Rel.ENEMY) {
            return Outcome.BLOCK_DIRECT_WAR;
        }
        if (currentRelation == Rel.ALLY && targetRelation == Rel.NEUTRAL) {
            return Outcome.RECORD_BREAK;
        }
        if (targetRelation == Rel.ENEMY && recentBreak) {
            return Outcome.PENALIZE;
        }
        return Outcome.NONE;
    }
}
