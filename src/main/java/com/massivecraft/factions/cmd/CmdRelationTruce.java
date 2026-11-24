package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Rel;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdRelationTruce extends FRelationCommand {
    public CmdRelationTruce() {
        aliases.add("truce");
        targetRelation = Rel.TRUCE;
    }
}
