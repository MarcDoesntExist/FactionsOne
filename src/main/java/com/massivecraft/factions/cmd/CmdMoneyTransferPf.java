package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.Color;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdMoneyTransferPf extends FCommand {
    public CmdMoneyTransferPf() {
        this.aliases.add("pf");

        this.requiredArgs.add("amount");
        this.requiredArgs.add("player");
        this.requiredArgs.add("faction");

        //this.optionalArgs.put("", "");

        this.permission = Permission.MONEY_P2F.node;
        this.setHelpShort("transfer main -> f");

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        double amount = this.argAsDouble(0, 0d);
        EconomyParticipator from = this.argAsBestFPlayerMatch(1);
        if (from == null)
            return;
        EconomyParticipator to = this.argAsFaction(2);
        if (to == null)
            return;

        boolean success = Econ.transferMoney(fme, from, to, amount);

        if (success && Conf.logMoneyTransactions)
            Main.main.log(Main.main.txt
                    .parse("%s transferred %s from the player \"%s\" to the faction \"%s\"",
                            fme.getName(), Econ.moneyString(amount), from.describeTo(null),
                            to.describeTo(null)));
    }
}
