package com.massivecraft.factions.event;

import com.massivecraft.factions.Faction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread before a shield is started or extended. Cancelling
 * it leaves the old shield expiry in place.
 *
 * @author MarcDoesntExists
 */
public class ShieldChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Faction faction;
    private final long oldUntil;
    private final long newUntil;
    private final int hours;
    private boolean cancelled;

    public ShieldChangeEvent(Faction faction, long oldUntil, long newUntil, int hours) {
        this.faction = faction;
        this.oldUntil = oldUntil;
        this.newUntil = newUntil;
        this.hours = hours;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Faction getFaction() {
        return this.faction;
    }

    public long getOldUntil() {
        return this.oldUntil;
    }

    public long getNewUntil() {
        return this.newUntil;
    }

    public int getHours() {
        return this.hours;
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
