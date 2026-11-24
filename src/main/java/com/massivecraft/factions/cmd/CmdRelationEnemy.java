package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Rel;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdRelationEnemy extends FRelationCommand {
    public CmdRelationEnemy() {
        aliases.add("enemy");
        targetRelation = Rel.ENEMY;
    }
}
