package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;

/**
 * @author MarcDoesntExists
 */
public class CmdTax extends FCommand {
    public final CmdTaxInfo cmdTaxInfo = new CmdTaxInfo();
    public final CmdTaxRun cmdTaxRun = new CmdTaxRun();

    public CmdTax() {
        super();
        this.aliases.add("tax");

        this.permission = Permission.TAX.node;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;

        this.setHelpShort("faction tax commands");
        this.helpLong.add(p.txt.parseTags("<i>Shows the faction tax status and lets admins run a tax period."));

        this.addSubCommand(this.cmdTaxInfo);
        this.addSubCommand(this.cmdTaxRun);
    }

    @Override
    public void perform() {
        this.commandChain.add(this);
        Main.main.cmdAutoHelp.execute(this.sender, this.args, this.commandChain);
    }
}
