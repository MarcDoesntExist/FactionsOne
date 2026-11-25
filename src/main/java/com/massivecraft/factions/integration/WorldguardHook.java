package com.massivecraft.factions.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loaded reflectively by {@link Worldguard} only when WorldGuard is present.
 * Keep every WorldGuard class reference in this class.
 *
 * @author MarcDoesntExists
 */
final class WorldguardHook implements Worldguard.Hook {
    private final WorldGuardPlugin wg = WorldGuardPlugin.inst();

    WorldguardHook() {
        if (wg == null) {
            throw new IllegalStateException("WorldGuard plugin instance is not available");
        }
    }

    private static RegionManager regionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
    }

    @Override
    public boolean isPVP(Player player) {
        RegionManager regionManager = regionManager(player.getWorld());
        if (regionManager == null) {
            return true;
        }

        ApplicableRegionSet set =
                regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
        return set.testState(wg.wrapPlayer(player), Flags.PVP);
    }

    @Override
    public boolean checkForRegionsInChunk(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }

        RegionManager regionManager = regionManager(world);
        if (regionManager == null) {
            return false;
        }

        Chunk chunk = world.getChunkAt(loc);
        int minChunkX = chunk.getX() << 4;
        int minChunkZ = chunk.getZ() << 4;
        int maxChunkX = minChunkX + 15;
        int maxChunkZ = minChunkZ + 15;

        BlockVector3 min = BlockVector3.at(minChunkX, world.getMinHeight(), minChunkZ);
        // WorldGuard cuboids use inclusive bounds; the world max height is exclusive.
        BlockVector3 max = BlockVector3.at(maxChunkX, world.getMaxHeight() - 1, maxChunkZ);
        ProtectedCuboidRegion region =
                new ProtectedCuboidRegion("wgfactionoverlapcheck", min, max);

        // Any region overlapping the chunk blocks a faction claim. PvP flags are
        // not merged here: WorldGuard's testState resolves overlapping flags by
        // region priority, with deny winning an equal-priority conflict.
        Map<String, ProtectedRegion> allregions = regionManager.getRegions();
        List<ProtectedRegion> overlaps =
                region.getIntersectingRegions(new ArrayList<ProtectedRegion>(allregions.values()));
        return overlaps != null && !overlaps.isEmpty();
    }
}
