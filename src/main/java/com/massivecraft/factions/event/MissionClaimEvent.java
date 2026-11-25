package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FeatureConfig;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread after mission validation and before money, points
 * or items are granted. Cancelling it leaves the mission unclaimed.
 *
 * @author MarcDoesntExists
 */
public class MissionClaimEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final FPlayer fplayer;
    private final Faction faction;
    private final FeatureConfig.MissionDefinition mission;
    private boolean cancelled;

    public MissionClaimEvent(FPlayer fplayer, Faction faction,
                             FeatureConfig.MissionDefinition mission) {
        this.fplayer = fplayer;
        this.faction = faction;
        this.mission = mission;
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

    public FeatureConfig.MissionDefinition getMission() {
        return this.mission;
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
