package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.TntBankService;

/**
 * Deposits TNT from the sender's inventory.
 *
 * @author MarcDoesntExists
 */
public final class CmdTntDeposit extends FCommand {

    public CmdTntDeposit() {
        super();
        this.aliases.add("deposit");
        this.requiredArgs.add("amount");
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        int amount = Math.max(0, this.argAsInt(0, 0));
        TntBankService.Result result = Main.main.tntBankService.deposit(this.fme, amount);
        if (result == TntBankService.Result.SUCCESS) {
            msg("<g>Deposited TNT. Bank: <h>%s<g>.", this.myFaction.getTntBank());
        } else {
            msg(TntBankService.message(result));
        }
    }
}
