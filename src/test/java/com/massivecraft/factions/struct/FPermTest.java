package com.massivecraft.factions.struct;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the default relation matrix of faction permissions.
 *
 * @author MarcDoesntExists
 */
class FPermTest {

    @Test
    void coLeaderGetsOperationalPermissions() {
        List<FPerm> operational = List.of(
                FPerm.BUILD,
                FPerm.DOOR,
                FPerm.BUTTON,
                FPerm.LEVER,
                FPerm.CONTAINER,
                FPerm.INVITE,
                FPerm.KICK,
                FPerm.SETHOME,
                FPerm.WITHDRAW,
                FPerm.TERRITORY,
                FPerm.CAPE,
                FPerm.ACCESS,
                FPerm.PERMS);

        for (FPerm perm : operational) {
            assertTrue(perm.defaultDefaultValue.contains(Rel.COLEADER), perm.name());
        }
    }

    @Test
    void coLeaderCannotDisbandTheFaction() {
        assertFalse(FPerm.DISBAND.defaultDefaultValue.contains(Rel.COLEADER));
        assertTrue(FPerm.DISBAND.defaultDefaultValue.contains(Rel.LEADER));
    }

    @Test
    void leaderKeepsEveryDefaultPermission() {
        for (FPerm perm : FPerm.values()) {
            if (perm == FPerm.PAINBUILD) {
                assertTrue(perm.defaultDefaultValue.isEmpty());
                continue;
            }
            assertTrue(perm.defaultDefaultValue.contains(Rel.LEADER), perm.name());
        }
    }
}
