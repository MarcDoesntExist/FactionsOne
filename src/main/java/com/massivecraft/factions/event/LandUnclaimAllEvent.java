package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
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
public class LandUnclaimAllEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Faction faction;
    private final FPlayer fplayer;
    private boolean cancelled;

    public LandUnclaimAllEvent(Faction f, FPlayer p) {
        faction = f;
        fplayer = p;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public Faction getFaction() {
        return faction;
    }

    public String getFactionId() {
        return faction.getId();
    }

    public String getFactionTag() {
        return faction.getTagPlain();
    }

    public FPlayer getFPlayer() {
        return fplayer;
    }

    public Player getPlayer() {
        return fplayer.getPlayer();
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
