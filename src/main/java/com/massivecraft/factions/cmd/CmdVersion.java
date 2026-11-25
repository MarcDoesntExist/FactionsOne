package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdVersion extends FCommand {
    public CmdVersion() {
        this.aliases.add("version");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        this.permission = Permission.VERSION.node;
        this.disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        msg("<i>You are running " + Main.main.getPluginMeta().getName() + " v"
                + Main.main.getPluginMeta().getVersion());
    }
}
