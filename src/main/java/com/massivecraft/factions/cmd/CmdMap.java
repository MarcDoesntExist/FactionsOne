package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.struct.Permission;
import org.bukkit.entity.Player;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdMap extends FCommand {
    public CmdMap() {
        super();
        this.aliases.add("map");

        //this.requiredArgs.add("");
        this.optionalArgs.put("on/off", "once");

        this.permission = Permission.MAP.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        if (this.argIsSet(0)) {
            if (this.argAsBool(0, !fme.isMapAutoUpdating())) {
                // Turn on

                // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
                if (!payForCommand(Conf.econCostMap, "to show the map", "for showing the map"))
                    return;

                fme.setMapAutoUpdating(true);
                msg("<i>Map auto update <green>ENABLED.");

                // Show the compact map in the action bar, not in chat.
                showActionBarMap();
            } else {
                // Turn off
                fme.setMapAutoUpdating(false);
                msg("<i>Map auto update <red>DISABLED.");
            }
        } else {
            // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
            if (!payForCommand(Conf.econCostMap, "to show the map", "for showing the map"))
                return;

            showMap();
        }
    }

    public void showMap() {
        sendMessage(
            Board.getMap(myFaction, new FLocation(fme), fme.getPlayer().getLocation().getYaw()));
    }

    public void showActionBarMap() {
        Player player = fme.getPlayer();
        if (player == null)
            return;
        player.sendActionBar(Board.getActionBarMap(fme, new FLocation(player)));
    }

}
