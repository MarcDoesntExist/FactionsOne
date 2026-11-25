package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionWarp;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread after warp validation and before the warmup starts.
 * Cancelling it prevents the teleport and does not consume the cooldown.
 *
 * @author MarcDoesntExists
 */
public class WarpTeleportEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final FPlayer fplayer;
    private final Faction faction;
    private final FactionWarp warp;
    private final int warmupSeconds;
    private boolean cancelled;

    public WarpTeleportEvent(FPlayer fplayer, Faction faction, FactionWarp warp,
                             int warmupSeconds) {
        this.fplayer = fplayer;
        this.faction = faction;
        this.warp = warp;
        this.warmupSeconds = warmupSeconds;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public FPlayer getFPlayer() {
        return this.fplayer;
    }

    public Faction getFaction() {
        return this.faction;
    }

    public FactionWarp getWarp() {
        return this.warp;
    }

    public int getWarmupSeconds() {
        return this.warmupSeconds;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
