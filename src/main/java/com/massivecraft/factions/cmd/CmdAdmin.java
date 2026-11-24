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
public class CmdAdmin extends FCommand {
    public CmdAdmin() {
        super();
        this.aliases.add("admin");

        //this.requiredArgs.add("");
        this.optionalArgs.put("on/off", "flip");

        this.permission = Permission.ADMIN.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        fme.setHasAdminMode(this.argAsBool(0, !fme.hasAdminMode()));

        if (fme.hasAdminMode()) {
            fme.msg("<i>You have enabled admin bypass mode.");
            Main.main.log(fme.getName() + " has ENABLED admin bypass mode.");
        } else {
            fme.msg("<i>You have disabled admin bypass mode.");
            Main.main.log(fme.getName() + " DISABLED admin bypass mode.");
        }
    }
}
