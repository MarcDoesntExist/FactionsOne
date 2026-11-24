package com.massivecraft.factions.event;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Rel;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FactionRelationEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Faction fsender;
    private final Faction ftarget;
    private final Rel foldrel;
    private final Rel frel;

    public FactionRelationEvent(Faction sender, Faction target, Rel oldrel, Rel rel) {
        fsender = sender;
        ftarget = target;
        foldrel = oldrel;
        frel = rel;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public Rel getOldRelation() {
        return foldrel;
    }

    public Rel getRelation() {
        return frel;
    }

    public Faction getFaction() {
        return fsender;
    }

    public Faction getTargetFaction() {
        return ftarget;
    }
}
