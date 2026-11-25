package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.Color;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdMoneyDeposit extends FCommand {

    public CmdMoneyDeposit() {
        super();
        this.aliases.add("d");
        this.aliases.add("deposit");

        this.requiredArgs.add("amount");
        this.optionalArgs.put("faction", "your");

        this.permission = Permission.MONEY_DEPOSIT.node;
        this.setHelpShort("deposit money");

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        double amount = this.argAsDouble(0, 0d);
        Faction faction = this.argAsFaction(1, myFaction);
        if (faction == null)
            return;
        if (!FPerm.BANK_DEPOSIT.has(fme, faction, true))
            return;
        boolean success = Econ.transferMoney(fme, fme, faction, amount);

        if (success && Conf.logMoneyTransactions)
            Main.main.log(Main.main.txt
                    .parse("%s deposited %s in the faction bank: %s", fme.getName(),
                            Econ.moneyString(amount), faction.describeTo(null)));
    }

}
