package com.massivecraft.factions.value;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes the value of spawners and container contents inside claimed chunks
 * in small main-thread slices. The values are derived state: they are rebuilt
 * after a restart, never serialized, and unloaded chunks keep their previous
 * value until they are loaded again.
 *
 * @author MarcDoesntExists
 */
public final class ClaimValueService {

    private final Main plugin;
    private final Map<String, ChunkRef> chunkRefs = new HashMap<String, ChunkRef>();
    private final ClaimValueTotals totals = new ClaimValueTotals();
    private final Deque<ChunkRef> queue = new ArrayDeque<ChunkRef>();
    private final Set<String> queued = new HashSet<String>();
    private int taskId = -1;
    private long lastFullScan;

    public ClaimValueService(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (this.taskId != -1) {
            return;
        }
        this.taskId = this.plugin.getServer().getScheduler()
                .runTaskTimer(this.plugin, this::tick, 1L, 1L).getTaskId();
    }

    public void close() {
        if (this.taskId != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }
        this.queue.clear();
        this.queued.clear();
        this.chunkRefs.clear();
        this.totals.clear();
    }

    /**
     * @return the last fully or partially computed value for the faction
     */
    public double value(String factionId) {
        return this.totals.value(factionId);
    }

    public void markDirty(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        markDirty(location.getWorld().getName(), location.getBlockX() >> 4,
                location.getBlockZ() >> 4);
    }

    public void markDirty(String world, int x, int z) {
        if (!Conf.claimValueEnabled || world == null) {
            return;
        }
        if ("0".equals(Board.getIdAt(new FLocation(world, x, z)))) {
            return;
        }
        enqueue(new ChunkRef(world, x, z));
    }

    /**
     * Queues a chunk without checking the current owner. Claim events fire
     * before the board is updated, so the normal dirty check would skip them.
     */
    public void markDirtyForced(String world, int x, int z) {
        if (!Conf.claimValueEnabled || world == null) {
            return;
        }
        enqueue(new ChunkRef(world, x, z));
    }

    public void markRemoved(String world, int x, int z) {
        if (world == null) {
            return;
        }
        removeChunk(key(world, x, z));
    }

    public void removeFaction(String factionId) {
        this.totals.removeFaction(factionId);
        this.chunkRefs.keySet().removeIf(key -> this.totals.factionOf(key) == null);
    }

    public void requestFullScan() {
        this.lastFullScan = 0L;
    }

    private void tick() {
        if (!Conf.claimValueEnabled) {
            return;
        }
        long now = System.currentTimeMillis();
        long refreshMillis = Math.max(1L, Conf.claimValueRefreshMinutes) * 60_000L;
        if (now - this.lastFullScan >= refreshMillis) {
            this.lastFullScan = now;
            enqueueAll();
        }
        int budget = Math.max(1, Conf.claimValueChunksPerTick);
        while (budget-- > 0 && !this.queue.isEmpty()) {
            ChunkRef ref = this.queue.poll();
            this.queued.remove(ref.key());
            scan(ref);
        }
    }

    private void enqueueAll() {
        for (ChunkRef ref : new ArrayList<ChunkRef>(this.chunkRefs.values())) {
            if ("0".equals(Board.getIdAt(new FLocation(ref.world(), ref.x(), ref.z())))) {
                removeChunk(ref.key());
            }
        }
        for (FLocation location : Board.flocationIds.keySet()) {
            enqueue(new ChunkRef(location.getWorldName(), (int) location.getX(),
                    (int) location.getZ()));
        }
    }

    private void enqueue(ChunkRef ref) {
        if (this.queued.add(ref.key())) {
            this.queue.add(ref);
        }
    }

    private void scan(ChunkRef ref) {
        String factionId = Board.getIdAt(new FLocation(ref.world(), ref.x(), ref.z()));
        if ("0".equals(factionId)) {
            removeChunk(ref.key());
            return;
        }
        Double value = compute(ref);
        if (value == null) {
            // The chunk is not loaded; the load listener will mark it again.
            return;
        }
        updateChunk(ref, factionId, value);
    }

    private Double compute(ChunkRef ref) {
        World world = Bukkit.getWorld(ref.world());
        if (world == null || !world.isChunkLoaded(ref.x(), ref.z())) {
            return null;
        }
        Chunk chunk = world.getChunkAt(ref.x(), ref.z());
        ClaimChunkData data = new ClaimChunkData();
        Set<Inventory> seen = Collections.newSetFromMap(new IdentityHashMap<Inventory, Boolean>());
        for (BlockState state : chunk.getTileEntities()) {
            Material material = state.getType();
            if (state instanceof CreatureSpawner spawner) {
                EntityType spawned = spawner.getSpawnedType();
                data.addSpawner(spawned == null ? "UNKNOWN" : spawned.name());
            } else if (state instanceof Container container) {
                data.addContainer(material.name());
                if (material != Material.ENDER_CHEST
                        && seen.add(container.getInventory())) {
                    for (ItemStack item : container.getInventory().getContents()) {
                        if (item != null) {
                            data.addItem(item.getType().name(), item.getAmount());
                        }
                    }
                }
            }
        }
        return ClaimValueCalculator.valueOf(data, Conf.claimValueSpawners,
                Conf.claimValueContainers, Conf.claimValueItems);
    }

    private void updateChunk(ChunkRef ref, String factionId, double value) {
        String key = ref.key();
        this.totals.put(key, factionId, value);
        this.chunkRefs.put(key, ref);
    }

    private void removeChunk(String key) {
        this.totals.remove(key);
        this.chunkRefs.remove(key);
        this.queued.remove(key);
    }

    private static String key(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    private record ChunkRef(String world, int x, int z) {
        String key() {
            return ClaimValueService.key(this.world, this.x, this.z);
        }
    }
}
