package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Main;

/**
 * Separate namespace for the server-administration commands. Keeping these
 * commands out of the player-facing {@code /f} help makes tab completion and
 * permission discovery smaller without changing the existing command classes.
 *
 * @author MarcDoesntExists
 */
public class FCmdAdminRoot extends FCommand {

    public final CmdAdmin cmdAdmin = new CmdAdmin();
    public final CmdReload cmdReload = new CmdReload();
    public final CmdSaveAll cmdSaveAll = new CmdSaveAll();
    public final CmdConfig cmdConfig = new CmdConfig();
    public final CmdPowerBoost cmdPowerBoost = new CmdPowerBoost();
    public final CmdDisband cmdDisband = new CmdDisband();
    public final CmdKick cmdKick = new CmdKick();
    public final CmdJoin cmdJoin = new CmdJoin();

    public FCmdAdminRoot() {
        super();
        this.aliases.add("fa");
        this.setHelpShort("The faction admin command");
        this.helpLong.add(p.txt.parseTags("<i>Administration commands for Factions."));

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
        disableOnLock = false;

        this.addSubCommand(Main.main.cmdAutoHelp);
        this.addSubCommand(this.cmdAdmin);
        this.addSubCommand(this.cmdReload);
        this.addSubCommand(this.cmdSaveAll);
        this.addSubCommand(this.cmdConfig);
        this.addSubCommand(this.cmdPowerBoost);
        this.addSubCommand(this.cmdDisband);
        this.addSubCommand(this.cmdKick);
        this.addSubCommand(this.cmdJoin);
    }

    @Override
    public void perform() {
        this.commandChain.add(this);
        Main.main.cmdAutoHelp.execute(this.sender, this.args, this.commandChain);
    }
}
