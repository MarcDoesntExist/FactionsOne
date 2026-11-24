package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FPlayerLeaveEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final PlayerLeaveReason reason;
    FPlayer FPlayer;
    Faction Faction;
    boolean cancelled = false;


    public FPlayerLeaveEvent(FPlayer p, Faction f, PlayerLeaveReason r) {
        FPlayer = p;
        Faction = f;
        reason = r;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public PlayerLeaveReason getReason() {
        return reason;
    }

    public FPlayer getFPlayer() {
        return FPlayer;
    }

    public Faction getFaction() {
        return Faction;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean c) {
        if (reason == PlayerLeaveReason.DISBAND || reason == PlayerLeaveReason.RESET) {
            cancelled = false;
            return;
        }
        cancelled = c;
    }

    public enum PlayerLeaveReason {
        KICKED, DISBAND, RESET, JOINOTHER, LEAVE
    }
}
