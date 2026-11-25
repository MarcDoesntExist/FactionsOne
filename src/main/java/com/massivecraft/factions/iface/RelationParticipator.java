package com.massivecraft.factions.iface;

import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.Color;
import net.kyori.adventure.text.Component;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public interface RelationParticipator {
    Component describeTo(RelationParticipator observer);

    Component describeTo(RelationParticipator observer, boolean ucfirst);

    Rel getRelationTo(RelationParticipator observer);

    Rel getRelationTo(RelationParticipator observer, boolean ignorePeaceful);

    Color getColorTo(RelationParticipator observer);
}
