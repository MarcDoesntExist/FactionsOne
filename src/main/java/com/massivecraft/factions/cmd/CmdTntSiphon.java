package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.TntBankService;

/**
 * Moves TNT from nearby dispensers into the faction TNT bank.
 *
 * @author MarcDoesntExists
 */
public final class CmdTntSiphon extends FCommand {

    public CmdTntSiphon() {
        super();
        this.aliases.add("siphon");
        this.optionalArgs.put("radius", "5");
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        int radius = Math.max(0, this.argAsInt(0, 5));
        TntBankService.Result result = Main.main.tntBankService.siphon(this.fme, radius);
        if (result == TntBankService.Result.SUCCESS) {
            msg("<g>Siphoned TNT. Bank: <h>%s<g>.", this.myFaction.getTntBank());
        } else {
            msg(TntBankService.message(result));
        }
    }
}
