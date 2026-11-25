package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

/**
 * Parent for faction TNT bank commands.
 *
 * @author MarcDoesntExists
 */
public final class CmdTnt extends FCommand {

    public final CmdTntDeposit cmdTntDeposit = new CmdTntDeposit();
    public final CmdTntWithdraw cmdTntWithdraw = new CmdTntWithdraw();
    public final CmdTntBalance cmdTntBalance = new CmdTntBalance();
    public final CmdTntFill cmdTntFill = new CmdTntFill();
    public final CmdTntSiphon cmdTntSiphon = new CmdTntSiphon();

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
        this.addSubCommand(this.cmdTntFill);
        this.addSubCommand(this.cmdTntSiphon);
    }

    @Override
    public void perform() {
        msg(Lang.tntUsage);
    }
}
