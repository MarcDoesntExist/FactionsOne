package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;

/**
 * Shows the faction TNT bank balance.
 *
 * @author MarcDoesntExists
 */
public final class CmdTntBalance extends FCommand {

    public CmdTntBalance() {
        super();
        this.aliases.add("balance");
        this.aliases.add("b");
        this.disableOnLock = false;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        if (!FPerm.TNT_DEPOSIT.has(this.fme, this.myFaction, false)
                && !FPerm.TNT_WITHDRAW.has(this.fme, this.myFaction, false)) {
            this.fme.msg("<b>Your faction does not allow you to view the TNT bank.");
            return;
        }
        msg("<gold>TNT bank: <white>%s/%s", this.myFaction.getTntBank(),
                Main.main.featureService.tntLimit(this.myFaction));
    }
}
