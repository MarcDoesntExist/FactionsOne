package com.massivecraft.factions;

import com.massivecraft.factions.event.BankTransactionEvent;
import com.massivecraft.factions.event.FactionRoleChangeEvent;
import com.massivecraft.factions.event.MissionClaimEvent;
import com.massivecraft.factions.event.ShieldChangeEvent;
import com.massivecraft.factions.event.UpgradePurchaseEvent;
import com.massivecraft.factions.event.WarpTeleportEvent;
import com.massivecraft.factions.struct.Rel;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The API events must start uncancelled and honour cancellation before any
 * service mutates state. The service side is covered by the smoke server.
 *
 * @author MarcDoesntExists
 */
class ApiEventCancelTest {

    @Test
    void allFeatureEventsAreCancellable() {
        List<Event> events = List.of(
                new BankTransactionEvent(null, BankTransactionEvent.Type.DEPOSIT, 10.0, "actor"),
                new WarpTeleportEvent(null, null, null, 3),
                new UpgradePurchaseEvent(null, null, null, 0, 10),
                new ShieldChangeEvent(null, 0L, 1L, 1),
                new FactionRoleChangeEvent(null, null, Rel.MEMBER, Rel.OFFICER, null),
                new MissionClaimEvent(null, null, null));
        for (Event event : events) {
            Cancellable cancellable = (Cancellable) event;
            assertFalse(cancellable.isCancelled());
            cancellable.setCancelled(true);
            assertTrue(cancellable.isCancelled());
        }
    }

    @Test
    void cancelledMessageIsStable() {
        assertTrue(FeatureService.message(FeatureService.Result.CANCELLED)
                .contains("cancelled"));
    }
}
