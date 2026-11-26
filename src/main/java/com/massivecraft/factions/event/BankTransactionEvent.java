package com.massivecraft.factions.event;

import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread before a bank movement is applied. Cancelling it
 * prevents the movement and leaves balances untouched.
 *
 * @author MarcDoesntExists
 */
public class BankTransactionEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Faction faction;
    private final Type type;
    private final double amount;
    private final String actor;
    private boolean cancelled;
    /**
     * @param faction faction whose account is touched, or null for a
     *                player-to-player movement
     * @param type    direction of the movement
     * @param amount  positive amount in currency units
     * @param actor   name of the player or system starting the movement
     */
    public BankTransactionEvent(Faction faction, Type type, double amount, String actor) {
        this.faction = faction;
        this.type = type;
        this.amount = amount;
        this.actor = actor;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Convenience helper for services that have already validated the amount.
     */
    public static boolean fire(Faction faction, Type type, double amount, String actor) {
        if (Bukkit.getServer() == null) {
            return true;
        }
        BankTransactionEvent event = new BankTransactionEvent(faction, type, amount, actor);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public Faction getFaction() {
        return this.faction;
    }

    public Type getType() {
        return this.type;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getActor() {
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

    public enum Type {
        DEPOSIT,
        WITHDRAW,
        TRANSFER
    }
}
