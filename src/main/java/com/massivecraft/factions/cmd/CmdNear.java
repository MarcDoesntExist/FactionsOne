package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.struct.TerritoryAccess;
import com.massivecraft.factions.zcore.Lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists the closest enemy or neutral factions by their nearest claim.
 *
 * @author MarcDoesntExists
 */
public class CmdNear extends FCommand {

    private static final int RESULT_LIMIT = 10;

    public CmdNear() {
        super();
        this.aliases.add("near");
        this.optionalArgs.put("radius", String.valueOf(Conf.nearDefaultRadiusChunks));

        this.permission = Permission.NEAR.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        int radius = this.argAsInt(0, Conf.nearDefaultRadiusChunks);
        radius = Math.max(1, Math.min(Conf.nearMaxRadiusChunks, radius));

        FLocation origin = new FLocation(this.me.getLocation());
        Map<String, Double> distances = new HashMap<String, Double>();
        Map<String, Faction> factions = new HashMap<String, Faction>();

        for (Map.Entry<FLocation, TerritoryAccess> entry : Board.flocationIds.entrySet()) {
            FLocation claim = entry.getKey();
            if (!claim.getWorldName().equals(origin.getWorldName())) {
                continue;
            }
            double distance = Math.hypot(claim.getX() - origin.getX(), claim.getZ() - origin.getZ());
            if (distance > radius) {
                continue;
            }

            Faction faction = Factions.i.get(entry.getValue().getHostFactionID());
            if (faction == null || !faction.isNormal() || faction == this.myFaction) {
                continue;
            }
            Rel relation = this.myFaction == null
                ? Rel.NEUTRAL
                : this.myFaction.getRelationTo(faction);
            if (relation != Rel.ENEMY && relation != Rel.NEUTRAL) {
                continue;
            }

            distances.merge(faction.getId(), distance, Math::min);
            factions.put(faction.getId(), faction);
        }

        if (distances.isEmpty()) {
            msg(Lang.nearEmpty);
            return;
        }

        List<String> ids = new ArrayList<String>(distances.keySet());
        ids.sort((left, right) -> Double.compare(distances.get(left), distances.get(right)));

        msg(Lang.nearHeader, radius);
        int shown = 0;
        for (String id : ids) {
            if (shown++ >= RESULT_LIMIT) {
                break;
            }
            Faction faction = factions.get(id);
            Rel relation = this.myFaction == null
                ? Rel.NEUTRAL
                : this.myFaction.getRelationTo(faction);
            msg(Lang.nearLine, faction.getTagPlain(), Math.round(distances.get(id)),
                relation.name().toLowerCase(java.util.Locale.ROOT));
        }
    }
}
