package com.massivecraft.factions.gui;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for menu-entry requirements.
 *
 * @author MarcDoesntExists
 */
class MenuRequirementTest {

    @Test
    void defaultRequirementAllowsEverything() {
        MenuRequirement requirement = MenuRequirement.allowAll();
        assertTrue(requirement.canView(null));
        assertTrue(requirement.canClick(null));
        assertTrue(requirement.canAct(null));
    }

    @Test
    void hiddenEntryIsNotActionable() {
        MenuRequirement requirement = new MenuRequirement(player -> false, player -> true);
        assertFalse(requirement.canView(null));
        assertTrue(requirement.canClick(null));
        assertFalse(requirement.canAct(null));
    }

    @Test
    void clickRequirementBlocksAction() {
        AtomicInteger clicks = new AtomicInteger();
        MenuRequirement requirement = new MenuRequirement(player -> true, player -> false);
        Player player = null;

        if (requirement.canAct(player)) {
            clicks.incrementAndGet();
        }
        assertTrue(requirement.canView(player));
        assertFalse(requirement.canAct(player));
        assertTrue(clicks.get() == 0);
    }
}
