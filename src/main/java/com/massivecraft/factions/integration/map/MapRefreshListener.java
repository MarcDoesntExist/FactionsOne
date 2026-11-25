package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.event.FactionCreateEvent;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.event.FactionRelationEvent;
import com.massivecraft.factions.event.FactionRenameEvent;
import com.massivecraft.factions.event.LandClaimEvent;
import com.massivecraft.factions.event.LandUnclaimAllEvent;
import com.massivecraft.factions.event.LandUnclaimEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Refreshes the web map only after the server has applied the mutation. The
 * debounce in {@link MapIntegration} collapses bursts of claims.
 *
 * @author MarcDoesntExists
 */
public final class MapRefreshListener implements Listener {
    private final MapIntegration integration;

    public MapRefreshListener(MapIntegration integration) {
        this.integration = integration;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaim(LandClaimEvent event) {
        integration.requestRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnclaim(LandUnclaimEvent event) {
        integration.requestRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnclaimAll(LandUnclaimAllEvent event) {
        integration.requestRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRelation(FactionRelationEvent event) {
        integration.requestRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(FactionCreateEvent event) {
        integration.requestRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(FactionDisbandEvent event) {
        integration.requestRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRename(FactionRenameEvent event) {
        integration.requestRefresh();
    }
}
