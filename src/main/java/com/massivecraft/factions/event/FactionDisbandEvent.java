package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import org.bukkit.entity.Player;
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
public class FactionDisbandEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final String id;
    private final Player sender;
    private boolean cancelled;

    public FactionDisbandEvent(Player sender, String factionId) {
        cancelled = false;
        this.sender = sender;
        this.id = factionId;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public Faction getFaction() {
        return Factions.i.get(id);
    }

    public FPlayer getFPlayer() {
        return FPlayers.i.get(sender);
    }

    public Player getPlayer() {
        return sender;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean c) {
        cancelled = c;
    }
}
