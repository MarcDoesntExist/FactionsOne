package com.massivecraft.factions.util;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runs one delayed teleport per player and cancels it when the player moves to
 * another block, takes damage or leaves.
 *
 * @author MarcDoesntExists
 */
public final class TeleportWarmupService implements Listener {

    private final Main plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    private record Pending(int taskId, Runnable onCancel) {
    }

    public TeleportWarmupService(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isPending(UUID playerId) {
        return this.pending.containsKey(playerId);
    }

    public void start(Player player, int seconds, Runnable action) {
        this.start(player, seconds, action, null);
    }

    public void start(Player player, int seconds, Runnable action, Runnable onCancel) {
        this.cancel(player.getUniqueId());
        if (seconds <= 0) {
            action.run();
            return;
        }
        UUID playerId = player.getUniqueId();
        int taskId = this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            Pending current = this.pending.remove(playerId);
            if (current == null) {
                return;
            }
            if (player.isOnline()) {
                action.run();
            } else if (current.onCancel() != null) {
                current.onCancel().run();
            }
        }, seconds * 20L).getTaskId();
        this.pending.put(playerId, new Pending(taskId, onCancel));
    }

    public void cancel(UUID playerId) {
        Pending pending = this.pending.remove(playerId);
        if (pending != null) {
            this.plugin.getServer().getScheduler().cancelTask(pending.taskId());
            if (pending.onCancel() != null) {
                pending.onCancel().run();
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!this.pending.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld() || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            this.cancel(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage(this.plugin.txt.parse(Lang.teleportCancelled));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.cancel(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.cancel(event.getPlayer().getUniqueId());
    }
}
