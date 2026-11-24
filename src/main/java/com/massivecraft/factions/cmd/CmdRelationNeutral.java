package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Rel;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdRelationNeutral extends FRelationCommand {
    public CmdRelationNeutral() {
        aliases.add("neutral");
        targetRelation = Rel.NEUTRAL;
    }
}
