package com.massivecraft.factions.iface;

import com.massivecraft.factions.struct.Rel;
import org.bukkit.ChatColor;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public interface RelationParticipator {
    String describeTo(RelationParticipator observer);

    String describeTo(RelationParticipator observer, boolean ucfirst);

    Rel getRelationTo(RelationParticipator observer);

    Rel getRelationTo(RelationParticipator observer, boolean ignorePeaceful);

    ChatColor getColorTo(RelationParticipator observer);
}
