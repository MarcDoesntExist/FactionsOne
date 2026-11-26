package com.massivecraft.factions.util;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.zcore.Lang;
import de.erethon.factionsone.api.FactionsAPI;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Resolves {@code /f stuck} in this order: faction home, the nearest own claim
 * (loaded asynchronously), then the world spawn.
 *
 * @author MarcDoesntExists
 */
public final class StuckService {

    private final Main plugin;
    private final TeleportWarmupService warmup;

    public StuckService(Main plugin, TeleportWarmupService warmup) {
        this.plugin = plugin;
        this.warmup = warmup;
    }

    private static Location safeSpot(Chunk chunk) {
        World world = chunk.getWorld();
        int x = (chunk.getX() << 4) + 8;
        int z = (chunk.getZ() << 4) + 8;
        Block highest = world.getHighestBlockAt(x, z);
        return new Location(world, x + 0.5, highest.getY() + 1.0, z + 0.5);
    }

    public void teleport(FPlayer fplayer) {
        Player player = fplayer.getPlayer();
        if (player == null) {
            return;
        }
        Faction faction = fplayer.getFaction();
        if (faction == null || !faction.isNormal()) {
            fplayer.msg(Lang.notInFaction);
            return;
        }

        if (faction.hasHome()) {
            this.start(fplayer, faction, faction.getHome());
            return;
        }

        FLocation nearest = this.nearestClaim(faction, new FLocation(player.getLocation()));
        if (nearest == null) {
            this.start(fplayer, faction, player.getWorld().getSpawnLocation());
            return;
        }

        World world = nearest.getWorld();
        if (world == null) {
            this.start(fplayer, faction, player.getWorld().getSpawnLocation());
            return;
        }

        fplayer.msg(Lang.stuckSearching);
        world.getChunkAtAsync((int) nearest.getX(), (int) nearest.getZ()).thenAccept(chunk ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (player.isOnline()) {
                    this.start(fplayer, faction, safeSpot(chunk));
                }
            }));
    }

    private void start(FPlayer fplayer, Faction faction, Location destination) {
        Player player = fplayer.getPlayer();
        if (player == null || destination == null) {
            return;
        }
        int seconds = Math.max(0, Conf.stuckWarmupSeconds);
        if (seconds > 0) {
            fplayer.msg(Lang.stuckWarmup, seconds);
        }
        this.warmup.start(player, seconds, () -> {
            player.teleport(destination);
            player.setFallDistance(0);
            faction.addLog(fplayer.getName(), "STUCK", destination.getWorld().getName()
                + " " + destination.getBlockX() + " " + destination.getBlockY()
                + " " + destination.getBlockZ());
            fplayer.msg(Lang.stuckTeleported);
        });
    }

    private FLocation nearestClaim(Faction faction, FLocation origin) {
        FLocation best = null;
        double bestDistance = Double.MAX_VALUE;
        FLocation fallback = null;
        for (FLocation claim : FactionsAPI.getFactionClaims(faction)) {
            if (fallback == null && claim.getWorld() != null) {
                fallback = claim;
            }
            if (!claim.getWorldName().equals(origin.getWorldName())) {
                continue;
            }
            double distance = Math.hypot(claim.getX() - origin.getX(), claim.getZ() - origin.getZ());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = claim;
            }
        }
        return best != null ? best : fallback;
    }
}
