package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Location;

import java.util.Locale;

/**
 * Shows the chunk the player stands in, its owner and the in-claim offset.
 *
 * @author MarcDoesntExists
 */
public class CmdCoords extends FCommand {

    public CmdCoords() {
        super();
        this.aliases.add("coords");
        this.aliases.add("coord");

        this.permission = Permission.COORDS.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        Location location = this.me.getLocation();
        FLocation chunk = new FLocation(location);
        Faction owner = Board.getFactionAt(chunk);
        Rel relation = this.myFaction == null
                ? Rel.NEUTRAL
                : this.myFaction.getRelationTo(owner);

        msg(Lang.coordsLine, chunk.getCoordString(), chunk.getWorldName(),
                owner.getTagPlain(), relation.name().toLowerCase(Locale.ROOT));
        msg(Lang.coordsBlock, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                location.getBlockX() & 15, location.getBlockZ() & 15);
    }
}
