package com.massivecraft.factions.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread after a faction paid its periodic tax.
 *
 * @author MarcDoesntExists
 */
public class FactionTaxCollectedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final String factionId;
    private final String factionTag;
    private final double amount;
    private final long paidAt;

    public FactionTaxCollectedEvent(String factionId, String factionTag, double amount, long paidAt) {
        this.factionId = factionId;
        this.factionTag = factionTag;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public String getFactionId() {
        return factionId;
    }

    public String getFactionTag() {
        return factionTag;
    }

    public double getAmount() {
        return amount;
    }

    public long getPaidAt() {
        return paidAt;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
