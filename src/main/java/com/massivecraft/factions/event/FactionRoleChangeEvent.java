package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Rel;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread before a player's faction role is written. The
 * actor may be null for system changes such as leadership replacement.
 *
 * @author MarcDoesntExists
 */
public class FactionRoleChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final FPlayer fplayer;
    private final Faction faction;
    private final Rel oldRole;
    private final Rel newRole;
    private final FPlayer actor;
    private boolean cancelled;

    public FactionRoleChangeEvent(FPlayer fplayer, Faction faction, Rel oldRole, Rel newRole,
                                  FPlayer actor) {
        this.fplayer = fplayer;
        this.faction = faction;
        this.oldRole = oldRole;
        this.newRole = newRole;
        this.actor = actor;
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

    public Rel getOldRole() {
        return this.oldRole;
    }

    public Rel getNewRole() {
        return this.newRole;
    }

    public FPlayer getActor() {
        return this.actor;
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
