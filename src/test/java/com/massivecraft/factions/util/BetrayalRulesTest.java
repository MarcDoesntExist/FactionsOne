package com.massivecraft.factions.util;

import com.massivecraft.factions.struct.Rel;
import org.junit.jupiter.api.Test;

import static com.massivecraft.factions.util.BetrayalRules.Outcome.BLOCK_DIRECT_WAR;
import static com.massivecraft.factions.util.BetrayalRules.Outcome.CLEAR_BREAK;
import static com.massivecraft.factions.util.BetrayalRules.Outcome.NONE;
import static com.massivecraft.factions.util.BetrayalRules.Outcome.PENALIZE;
import static com.massivecraft.factions.util.BetrayalRules.Outcome.RECORD_BREAK;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Headless coverage for the allegiance-betrayal policy.
 *
 * @author MarcDoesntExists
 */
class BetrayalRulesTest {

    @Test
    void disabledRulesNeverInterfere() {
        assertEquals(NONE, BetrayalRules.classify(Rel.ALLY, Rel.ENEMY, false, false));
        assertEquals(NONE, BetrayalRules.classify(Rel.ALLY, Rel.ENEMY, true, false));
        assertEquals(NONE, BetrayalRules.classify(Rel.ALLY, Rel.NEUTRAL, false, false));
    }

    @Test
    void warOnAnAllyRequiresNeutralFirst() {
        assertEquals(BLOCK_DIRECT_WAR, BetrayalRules.classify(Rel.ALLY, Rel.ENEMY, false, true));
    }

    @Test
    void breakingAnAllianceIsRecorded() {
        assertEquals(RECORD_BREAK, BetrayalRules.classify(Rel.ALLY, Rel.NEUTRAL, false, true));
    }

    @Test
    void warAfterABrokenAllianceIsBetrayal() {
        assertEquals(PENALIZE, BetrayalRules.classify(Rel.NEUTRAL, Rel.ENEMY, true, true));
        assertEquals(PENALIZE, BetrayalRules.classify(Rel.TRUCE, Rel.ENEMY, true, true));
    }

    @Test
    void warWithoutAWindowIsNormal() {
        assertEquals(NONE, BetrayalRules.classify(Rel.NEUTRAL, Rel.ENEMY, false, true));
    }

    @Test
    void allyingAgainClearsTheWindow() {
        assertEquals(CLEAR_BREAK, BetrayalRules.classify(Rel.NEUTRAL, Rel.ALLY, true, true));
    }

    @Test
    void peacefulBreakDoesNotCountAsWar() {
        assertEquals(NONE, BetrayalRules.classify(Rel.ALLY, Rel.TRUCE, false, true));
    }
}
