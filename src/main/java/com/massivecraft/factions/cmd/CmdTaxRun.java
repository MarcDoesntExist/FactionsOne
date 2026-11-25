package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

/**
 * Runs one faction-tax period. Usable from the console and by admins; the
 * optional {@code force} argument bypasses the interval check.
 *
 * @author MarcDoesntExists
 */
public class CmdTaxRun extends FCommand {

    public CmdTaxRun() {
        super();
        this.aliases.add("run");
        this.optionalArgs.put("force", "false");

        this.permission = Permission.TAX_RUN.node;
        this.disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        if (!Conf.taxEnabled || Main.main.factionTaxService == null) {
            msg(Lang.taxDisabled);
            return;
        }
        boolean force = "force".equalsIgnoreCase(this.argAsString(0, ""));
        int charged = Main.main.factionTaxService.run(force);
        if (charged == 0 && !force) {
            msg(Lang.taxRunSkipped);
            return;
        }
        msg(Lang.taxRunResult, charged);
    }
}
