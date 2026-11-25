package com.massivecraft.factions.util;

import com.massivecraft.factions.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Sends fake blocks to a player. The changes are queued per player and sent in
 * a single multi-block packet at the end of the tick, instead of one packet per
 * block, so callers cannot forget to flush. {@link #flush(Player)} remains
 * available for an immediate send. Clearing restores the real blocks in one
 * batch too.
 *
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class VisualizeUtil {
    protected static final Map<UUID, Set<Location>> playerLocations = new HashMap<>();
    private static final Map<UUID, Map<Location, BlockData>> pending = new HashMap<>();

    public static Set<Location> getPlayerLocations(Player player) {
        return getPlayerLocations(player.getUniqueId());
    }

    @Deprecated
    public static Set<Location> getPlayerLocations(String playerName) {
        return getPlayerLocations(Bukkit.getOfflinePlayer(playerName).getUniqueId());
    }

    public static Set<Location> getPlayerLocations(UUID playerId) {
        Set<Location> ret = playerLocations.get(playerId);
        if (ret == null) {
            ret = new HashSet<>();
            playerLocations.put(playerId, ret);
        }
        return ret;
    }

    // -------------------------------------------- //
    // SINGLE
    // -------------------------------------------- //
    public static void addLocation(Player player, Location location, Material type, byte data) {
        addLocation(player, location, type);
    }

    public static void addLocation(Player player, Location location, Material type) {
        addLocation(player, location, type.createBlockData());
    }

    public static void addLocation(Player player, Location location, BlockData blockData) {
        UUID playerId = player.getUniqueId();
        getPlayerLocations(playerId).add(location);

        Map<Location, BlockData> changes = pending.get(playerId);
        if (changes == null) {
            changes = new LinkedHashMap<>();
            pending.put(playerId, changes);
            scheduleFlush(playerId);
        }
        changes.put(location, blockData);
    }

    /**
     * Queues one flush for the next tick. The pending map itself is the guard,
     * so adding hundreds of blocks still results in a single batch.
     */
    private static void scheduleFlush(UUID playerId) {
        Bukkit.getScheduler().runTask(Main.main, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                pending.remove(playerId);
                return;
            }
            flush(player);
        });
    }

    // -------------------------------------------- //
    // MANY
    // -------------------------------------------- //

    public static void addLocations(Player player, Map<Location, Material> locationMaterials) {
        for (Map.Entry<Location, Material> entry : locationMaterials.entrySet()) {
            addLocation(player, entry.getKey(), entry.getValue());
        }
    }

    public static void addLocations(Player player, Collection<Location> locations, Material type) {
        BlockData blockData = type.createBlockData();
        for (Location location : locations) {
            addLocation(player, location, blockData);
        }
    }

    public static void addBlocks(Player player, Collection<Block> blocks, Material type) {
        BlockData blockData = type.createBlockData();
        for (Block block : blocks) {
            addLocation(player, block.getLocation(), blockData);
        }
    }

    // -------------------------------------------- //
    // SEND
    // -------------------------------------------- //

    /**
     * Sends every fake block queued for the player as one packet. Additions are
     * flushed automatically on the next tick; call this directly only when the
     * batch must become visible immediately.
     */
    public static void flush(Player player) {
        Map<Location, BlockData> changes = pending.remove(player.getUniqueId());
        if (changes == null || changes.isEmpty()) {
            return;
        }

        List<BlockState> states = new ArrayList<>(changes.size());
        for (Map.Entry<Location, BlockData> entry : changes.entrySet()) {
            BlockState state = entry.getKey().getBlock().getState();
            state.setBlockData(entry.getValue());
            states.add(state);
        }
        player.sendBlockChanges(states);
    }

    // -------------------------------------------- //
    // CLEAR
    // -------------------------------------------- //

    /**
     * Restores the real blocks (one batch) and forgets the player's visualization.
     */
    public static void clear(Player player) {
        UUID playerId = player.getUniqueId();
        pending.remove(playerId);

        Set<Location> locations = playerLocations.remove(playerId);
        if (locations == null || locations.isEmpty()) {
            return;
        }

        List<BlockState> states = new ArrayList<>(locations.size());
        for (Location location : locations) {
            states.add(location.getWorld().getBlockAt(location).getState());
        }
        player.sendBlockChanges(states);
    }

    /**
     * Drops all queued and tracked blocks without sending packets. Used on quit.
     */
    public static void forget(Player player) {
        UUID playerId = player.getUniqueId();
        pending.remove(playerId);
        playerLocations.remove(playerId);
    }
}
