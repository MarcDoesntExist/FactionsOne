package com.massivecraft.factions.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread when a faction cannot pay its periodic tax.
 *
 * @author MarcDoesntExists
 */
public class FactionTaxFailedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final String factionId;
    private final String factionTag;
    private final double amount;
    private final int arrears;
    private final String reason;

    public FactionTaxFailedEvent(String factionId, String factionTag, double amount, int arrears, String reason) {
        this.factionId = factionId;
        this.factionTag = factionTag;
        this.amount = amount;
        this.arrears = arrears;
        this.reason = reason;
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

    public int getArrears() {
        return arrears;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
