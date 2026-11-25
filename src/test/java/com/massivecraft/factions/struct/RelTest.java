package com.massivecraft.factions.struct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for role/relationship parsing and ordering.
 *
 * @author MarcDoesntExists
 */
class RelTest {

    @Test
    void fullNamesParseToTheirRole() {
        assertEquals(Rel.LEADER, Rel.parse("leader"));
        assertEquals(Rel.COLEADER, Rel.parse("coleader"));
        assertEquals(Rel.OFFICER, Rel.parse("officer"));
        assertEquals(Rel.MEMBER, Rel.parse("member"));
        assertEquals(Rel.RECRUIT, Rel.parse("recruit"));
        assertEquals(Rel.ALLY, Rel.parse("ally"));
        assertEquals(Rel.TRUCE, Rel.parse("truce"));
        assertEquals(Rel.NEUTRAL, Rel.parse("neutral"));
        assertEquals(Rel.ENEMY, Rel.parse("enemy"));
    }

    @Test
    void parsingIsCaseInsensitive() {
        assertEquals(Rel.LEADER, Rel.parse("LEADER"));
        assertEquals(Rel.ALLY, Rel.parse("AlLy"));
        assertEquals(Rel.ENEMY, Rel.parse("eNeMy"));
    }

    @Test
    void firstCharacterIsEnoughForShortInput() {
        assertEquals(Rel.LEADER, Rel.parse("l"));
        assertEquals(Rel.COLEADER, Rel.parse("c"));
        assertEquals(Rel.OFFICER, Rel.parse("o"));
        assertEquals(Rel.MEMBER, Rel.parse("m"));
        assertEquals(Rel.RECRUIT, Rel.parse("r"));
        assertEquals(Rel.ALLY, Rel.parse("a"));
        assertEquals(Rel.TRUCE, Rel.parse("t"));
        assertEquals(Rel.NEUTRAL, Rel.parse("n"));
        assertEquals(Rel.ENEMY, Rel.parse("e"));
    }

    @Test
    void legacyRoleNamesStillLoad() {
        assertEquals(Rel.LEADER, Rel.parse("admin"));
        assertEquals(Rel.OFFICER, Rel.parse("moderator"));
        assertEquals(Rel.MEMBER, Rel.parse("normal"));
    }

    @Test
    void unknownOrEmptyInputReturnsNull() {
        assertNull(Rel.parse(null));
        assertNull(Rel.parse(""));
        assertNull(Rel.parse("x"));
        assertNull(Rel.parse("?"));
    }

    @Test
    void rolesAreOrderedFromLeaderToEnemy() {
        assertTrue(Rel.LEADER.isMoreThan(Rel.COLEADER));
        assertTrue(Rel.COLEADER.isMoreThan(Rel.OFFICER));
        assertTrue(Rel.LEADER.isAtLeast(Rel.COLEADER));
        assertTrue(Rel.COLEADER.isAtLeast(Rel.OFFICER));
        assertTrue(Rel.COLEADER.isAtMost(Rel.LEADER));
        assertTrue(Rel.OFFICER.isLessThan(Rel.COLEADER));
        assertTrue(Rel.OFFICER.isMoreThan(Rel.MEMBER));
        assertTrue(Rel.MEMBER.isMoreThan(Rel.RECRUIT));
        assertTrue(Rel.RECRUIT.isMoreThan(Rel.ALLY));
        assertTrue(Rel.ALLY.isMoreThan(Rel.TRUCE));
        assertTrue(Rel.TRUCE.isMoreThan(Rel.NEUTRAL));
        assertTrue(Rel.NEUTRAL.isMoreThan(Rel.ENEMY));

        assertTrue(Rel.ENEMY.isLessThan(Rel.NEUTRAL));
        assertTrue(Rel.LEADER.isAtLeast(Rel.LEADER));
        assertTrue(Rel.TRUCE.isAtMost(Rel.ALLY));
        assertFalse(Rel.ENEMY.isAtLeast(Rel.NEUTRAL));
        assertFalse(Rel.LEADER.isAtMost(Rel.OFFICER));
    }

    @Test
    void descriptionsAreAvailableForEntityAndFactionForms() {
        assertEquals("your faction leader", Rel.LEADER.getDescPlayerOne());
        assertEquals("a co-leader in your faction", Rel.COLEADER.getDescPlayerOne());
        assertEquals("co-leaders in your faction", Rel.COLEADER.getDescPlayerMany());
        assertEquals("enemies", Rel.ENEMY.getDescPlayerMany());
        assertEquals("a neutral faction", Rel.NEUTRAL.getDescFactionOne());
        assertEquals("allied factions", Rel.ALLY.getDescFactionMany());
    }
}
