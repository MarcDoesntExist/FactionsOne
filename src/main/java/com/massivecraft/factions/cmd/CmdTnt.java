package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;

/**
 * Parent for faction TNT bank commands.
 *
 * @author MarcDoesntExists
 */
public final class CmdTnt extends FCommand {

    public final CmdTntDeposit cmdTntDeposit = new CmdTntDeposit();
    public final CmdTntWithdraw cmdTntWithdraw = new CmdTntWithdraw();
    public final CmdTntBalance cmdTntBalance = new CmdTntBalance();

    public CmdTnt() {
        super();
        this.aliases.add("tnt");
        this.permission = Permission.TNT.node;
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
        this.addSubCommand(this.cmdTntDeposit);
        this.addSubCommand(this.cmdTntWithdraw);
        this.addSubCommand(this.cmdTntBalance);
    }

    @Override
    public void perform() {
        msg("<i>Use /f tnt deposit|withdraw|balance <amount>.");
    }
}
