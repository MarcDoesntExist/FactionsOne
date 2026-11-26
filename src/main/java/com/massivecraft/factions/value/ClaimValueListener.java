package com.massivecraft.factions.value;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.event.LandClaimEvent;
import com.massivecraft.factions.event.LandUnclaimAllEvent;
import com.massivecraft.factions.event.LandUnclaimEvent;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Marks claimed chunks dirty when their valued contents can change. The scan
 * itself stays in {@link ClaimValueService}.
 *
 * @author MarcDoesntExists
 */
public final class ClaimValueListener implements Listener {

    private final ClaimValueService service;

    public ClaimValueListener(ClaimValueService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        this.service.markDirty(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        this.service.markDirty(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        this.service.markDirtyThrottled(event.getSource().getLocation());
        this.service.markDirtyThrottled(event.getDestination().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getLocation() != null) {
            this.service.markDirty(event.getInventory().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaim(LandClaimEvent event) {
        FLocation location = event.getLocation();
        this.service.markDirtyForced(location.getWorldName(), (int) location.getX(),
                (int) location.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnclaim(LandUnclaimEvent event) {
        FLocation location = event.getLocation();
        this.service.markRemoved(location.getWorldName(), (int) location.getX(),
                (int) location.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnclaimAll(LandUnclaimAllEvent event) {
        this.service.removeFaction(event.getFactionId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(FactionDisbandEvent event) {
        this.service.removeFaction(event.getFaction().getId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        this.service.markDirty(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
}
