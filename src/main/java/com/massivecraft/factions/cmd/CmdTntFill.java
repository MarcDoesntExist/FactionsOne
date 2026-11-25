package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.TntBankService;

/**
 * Fills dispensers around the sender from the faction TNT bank.
 *
 * @author MarcDoesntExists
 */
public final class CmdTntFill extends FCommand {

    public CmdTntFill() {
        super();
        this.aliases.add("fill");
        this.requiredArgs.add("radius");
        this.requiredArgs.add("amount");
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        int radius = Math.max(0, this.argAsInt(0, 0));
        int amount = Math.max(0, this.argAsInt(1, 0));
        TntBankService.Result result = Main.main.tntBankService.fill(this.fme, radius, amount);
        if (result == TntBankService.Result.SUCCESS) {
            msg("<g>Filled dispensers. Bank: <h>%s<g>.", this.myFaction.getTntBank());
        } else {
            msg(TntBankService.message(result));
        }
    }
}
