package com.massivecraft.factions.iface;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public interface EconomyParticipator extends RelationParticipator {
    String getAccountId();

    void msg(String str, Object... args);
}
