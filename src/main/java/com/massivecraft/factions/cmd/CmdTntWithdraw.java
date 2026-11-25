package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.TntBankService;

/**
 * Withdraws TNT from the faction bank.
 *
 * @author MarcDoesntExists
 */
public final class CmdTntWithdraw extends FCommand {

    public CmdTntWithdraw() {
        super();
        this.aliases.add("withdraw");
        this.requiredArgs.add("amount");
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        int amount = Math.max(0, this.argAsInt(0, 0));
        TntBankService.Result result = Main.main.tntBankService.withdraw(this.fme, amount);
        if (result == TntBankService.Result.SUCCESS) {
            msg("<g>Withdrew TNT. Bank: <h>%s<g>.", this.myFaction.getTntBank());
        } else {
            msg(TntBankService.message(result));
        }
    }
}
