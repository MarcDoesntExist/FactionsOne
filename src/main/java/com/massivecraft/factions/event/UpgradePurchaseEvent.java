package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FeatureConfig;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread after cost validation and before the faction points
 * are deducted. Cancelling it leaves points and upgrade levels unchanged.
 *
 * @author MarcDoesntExists
 */
public class UpgradePurchaseEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final FPlayer fplayer;
    private final Faction faction;
    private final FeatureConfig.UpgradeDefinition definition;
    private final int currentLevel;
    private final int cost;
    private boolean cancelled;

    public UpgradePurchaseEvent(FPlayer fplayer, Faction faction,
                                FeatureConfig.UpgradeDefinition definition,
                                int currentLevel, int cost) {
        this.fplayer = fplayer;
        this.faction = faction;
        this.definition = definition;
        this.currentLevel = currentLevel;
        this.cost = cost;
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

    public FeatureConfig.UpgradeDefinition getDefinition() {
        return this.definition;
    }

    public int getCurrentLevel() {
        return this.currentLevel;
    }

    public int getCost() {
        return this.cost;
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
