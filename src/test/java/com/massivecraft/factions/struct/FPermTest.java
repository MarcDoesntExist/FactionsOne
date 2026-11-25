package com.massivecraft.factions.struct;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                FPerm.PERMS,
                FPerm.WARP,
                FPerm.SETWARP,
                FPerm.FLY,
                FPerm.BANK_DEPOSIT,
                FPerm.BANK_WITHDRAW,
                FPerm.TNT_DEPOSIT,
                FPerm.TNT_WITHDRAW,
                FPerm.UPGRADE,
                FPerm.SHOP,
                FPerm.MISSIONS,
                FPerm.CHAT_ALLY,
                FPerm.LOGS,
                FPerm.BAN,
                FPerm.MENU_MEMBERS,
                FPerm.MENU_PERMS);

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

    @Test
    void extendedPermissionsHaveSensibleDefaults() {
        assertTrue(FPerm.WARP.defaultDefaultValue.contains(Rel.MEMBER));
        assertFalse(FPerm.WARP.defaultDefaultValue.contains(Rel.RECRUIT));
        assertTrue(FPerm.SETWARP.defaultDefaultValue.contains(Rel.OFFICER));
        assertFalse(FPerm.SETWARP.defaultDefaultValue.contains(Rel.MEMBER));
        assertTrue(FPerm.FLY.defaultDefaultValue.contains(Rel.MEMBER));
        assertFalse(FPerm.FLY.defaultDefaultValue.contains(Rel.RECRUIT));

        assertTrue(FPerm.BANK_DEPOSIT.defaultDefaultValue.contains(Rel.RECRUIT));
        assertFalse(FPerm.BANK_WITHDRAW.defaultDefaultValue.contains(Rel.RECRUIT));
        assertTrue(FPerm.TNT_DEPOSIT.defaultDefaultValue.contains(Rel.RECRUIT));
        assertFalse(FPerm.TNT_WITHDRAW.defaultDefaultValue.contains(Rel.RECRUIT));

        assertTrue(FPerm.UPGRADE.defaultDefaultValue.contains(Rel.COLEADER));
        assertFalse(FPerm.UPGRADE.defaultDefaultValue.contains(Rel.OFFICER));
        assertTrue(FPerm.BAN.defaultDefaultValue.contains(Rel.OFFICER));
        assertTrue(FPerm.LOGS.defaultDefaultValue.contains(Rel.OFFICER));
        assertTrue(FPerm.MENU_MEMBERS.defaultDefaultValue.contains(Rel.RECRUIT));
        assertTrue(FPerm.MENU_PERMS.defaultDefaultValue.contains(Rel.COLEADER));
        assertFalse(FPerm.MENU_PERMS.defaultDefaultValue.contains(Rel.OFFICER));
    }

    @Test
    void parseAcceptsExtendedNamesAndLegacyShortForms() {
        assertEquals(FPerm.WARP, FPerm.parse("warp"));
        assertEquals(FPerm.WARP, FPerm.parse("wa"));
        assertEquals(FPerm.WITHDRAW, FPerm.parse("w"));
        assertEquals(FPerm.SETWARP, FPerm.parse("setwarp"));
        assertEquals(FPerm.SETHOME, FPerm.parse("s"));
        assertEquals(FPerm.SHOP, FPerm.parse("shop"));
        assertEquals(FPerm.BANK_DEPOSIT, FPerm.parse("bank_deposit"));
        assertEquals(FPerm.BANK_WITHDRAW, FPerm.parse("BANK_WITHDRAW"));
        assertEquals(FPerm.BAN, FPerm.parse("ban"));
        assertEquals(FPerm.FLY, FPerm.parse("fly"));
        assertEquals(FPerm.TNT_DEPOSIT, FPerm.parse("tntdeposit"));
        assertEquals(FPerm.TERRITORY, FPerm.parse("t"));
        assertEquals(FPerm.UPGRADE, FPerm.parse("u"));
        assertEquals(FPerm.MISSIONS, FPerm.parse("missions"));
        assertEquals(FPerm.CHAT_ALLY, FPerm.parse("chatally"));
        assertEquals(FPerm.LOGS, FPerm.parse("logs"));
        assertEquals(FPerm.LEVER, FPerm.parse("l"));
        assertEquals(FPerm.MENU_MEMBERS, FPerm.parse("menu_members"));
        assertEquals(FPerm.MENU_PERMS, FPerm.parse("menuperms"));
    }

    @Test
    void parseRejectsUnknownOrEmptyNames() {
        assertNull(FPerm.parse(null));
        assertNull(FPerm.parse(""));
        assertNull(FPerm.parse("removed_permission"));
    }
}
