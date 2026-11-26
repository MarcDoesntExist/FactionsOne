package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;

import java.util.Collections;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FCmdRoot extends FCommand {
    public CmdAccess cmdAccess = new CmdAccess();
    public CmdBan cmdBan = new CmdBan();
    public CmdBanList cmdBanList = new CmdBanList();
    public CmdLeader cmdLeader = new CmdLeader();
    public CmdAutoClaim cmdAutoClaim = new CmdAutoClaim();
    public CmdAdmin cmdBypass = new CmdAdmin();
    public CmdClaim cmdClaim = new CmdClaim();
    public CmdConfig cmdConfig = new CmdConfig();
    public CmdCreate cmdCreate = new CmdCreate();
    public CmdDeinvite cmdDeinvite = new CmdDeinvite();
    public CmdDemote cmdDemote = new CmdDemote();
    public CmdDescription cmdDescription = new CmdDescription();
    public CmdDisband cmdDisband = new CmdDisband();
    public CmdFlag cmdFlag = new CmdFlag();
    public CmdHome cmdHome = new CmdHome();
    public CmdInvite cmdInvite = new CmdInvite();
    public CmdJoin cmdJoin = new CmdJoin();
    public CmdKick cmdKick = new CmdKick();
    public CmdLeave cmdLeave = new CmdLeave();
    public CmdList cmdList = new CmdList();
    public CmdLock cmdLock = new CmdLock();
    public CmdMail cmdMail = new CmdMail();
    public CmdMap cmdMap = new CmdMap();
    public CmdOfficer cmdOfficer = new CmdOfficer();
    public CmdMoney cmdMoney = new CmdMoney();
    public CmdMenu cmdMenu = new CmdMenu();
    public CmdWarp cmdWarp = new CmdWarp();
    public CmdSetWarp cmdSetWarp = new CmdSetWarp();
    public CmdShield cmdShield = new CmdShield();
    public CmdDelWarp cmdDelWarp = new CmdDelWarp();
    public CmdWarps cmdWarps = new CmdWarps();
    public CmdFly cmdFly = new CmdFly();
    public CmdTnt cmdTnt = new CmdTnt();
    public CmdUpgrade cmdUpgrade = new CmdUpgrade();
    public CmdShop cmdShop = new CmdShop();
    public CmdMissions cmdMissions = new CmdMissions();
    public CmdChat cmdChat = new CmdChat();
    public CmdLogs cmdLogs = new CmdLogs();
    public CmdOpen cmdOpen = new CmdOpen();
    public CmdPerm cmdPerm = new CmdPerm();
    public CmdPower cmdPower = new CmdPower();
    public CmdPowerBoost cmdPowerBoost = new CmdPowerBoost();
    public CmdPromote cmdPromote = new CmdPromote();
    public CmdRelationAlly cmdRelationAlly = new CmdRelationAlly();
    public CmdRelationEnemy cmdRelationEnemy = new CmdRelationEnemy();
    public CmdRelationNeutral cmdRelationNeutral = new CmdRelationNeutral();
    public CmdRelationTruce cmdRelationTruce = new CmdRelationTruce();
    public CmdReload cmdReload = new CmdReload();
    public CmdSaveAll cmdSaveAll = new CmdSaveAll();
    public CmdSeeChunk cmdSeeChunks = new CmdSeeChunk();
    public CmdSethome cmdSethome = new CmdSethome();
    public CmdShow cmdShow = new CmdShow();
    public CmdTag cmdTag = new CmdTag();
    public CmdTax cmdTax = new CmdTax();
    public CmdTop cmdTop = new CmdTop();
    public CmdStatus cmdStatus = new CmdStatus();
    public CmdCoords cmdCoords = new CmdCoords();
    public CmdNear cmdNear = new CmdNear();
    public CmdStuck cmdStuck = new CmdStuck();
    public CmdTitle cmdTitle = new CmdTitle();
    public CmdUnban cmdUnban = new CmdUnban();
    public CmdUnclaim cmdUnclaim = new CmdUnclaim();
    public CmdUnclaimall cmdUnclaimall = new CmdUnclaimall();
    public CmdVersion cmdVersion = new CmdVersion();

    public FCmdRoot() {
        super();
        this.aliases.addAll(Conf.baseCommandAliases);
        this.aliases
            .removeAll(Collections.singletonList(null));  // remove any nulls from extra commas

        //this.requiredArgs.add("");
        //this.optionalArgs.put("","")

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;

        this.disableOnLock = false;

        this.setHelpShort("The faction base command");
        this.helpLong.add(p.txt.parseTags("<i>This command contains all faction stuff."));

        this.addSubCommand(Main.main.cmdAutoHelp);
        this.addSubCommand(this.cmdList);
        this.addSubCommand(this.cmdShow);
        this.addSubCommand(this.cmdPower);
        this.addSubCommand(this.cmdJoin);
        this.addSubCommand(this.cmdLeave);
        this.addSubCommand(this.cmdHome);
        this.addSubCommand(this.cmdCreate);
        this.addSubCommand(this.cmdSethome);
        this.addSubCommand(this.cmdTag);
        this.addSubCommand(this.cmdTax);
        this.addSubCommand(this.cmdTop);
        this.addSubCommand(this.cmdStatus);
        this.addSubCommand(this.cmdCoords);
        this.addSubCommand(this.cmdNear);
        this.addSubCommand(this.cmdStuck);
        this.addSubCommand(this.cmdDemote);
        this.addSubCommand(this.cmdDescription);
        this.addSubCommand(this.cmdPerm);
        this.addSubCommand(this.cmdFlag);
        this.addSubCommand(this.cmdInvite);
        this.addSubCommand(this.cmdDeinvite);
        this.addSubCommand(this.cmdOpen);
        this.addSubCommand(this.cmdMoney);
        this.addSubCommand(this.cmdMail);
        this.addSubCommand(this.cmdMenu);
        this.addSubCommand(this.cmdWarp);
        this.addSubCommand(this.cmdSetWarp);
        this.addSubCommand(this.cmdShield);
        this.addSubCommand(this.cmdDelWarp);
        this.addSubCommand(this.cmdWarps);
        this.addSubCommand(this.cmdFly);
        this.addSubCommand(this.cmdTnt);
        this.addSubCommand(this.cmdUpgrade);
        this.addSubCommand(this.cmdShop);
        this.addSubCommand(this.cmdMissions);
        this.addSubCommand(this.cmdChat);
        this.addSubCommand(this.cmdLogs);
        this.addSubCommand(this.cmdClaim);
        this.addSubCommand(this.cmdAutoClaim);
        this.addSubCommand(this.cmdUnclaim);
        this.addSubCommand(this.cmdUnclaimall);
        this.addSubCommand(this.cmdAccess);
        this.addSubCommand(this.cmdBan);
        this.addSubCommand(this.cmdUnban);
        this.addSubCommand(this.cmdBanList);
        this.addSubCommand(this.cmdKick);
        this.addSubCommand(this.cmdOfficer);
        this.addSubCommand(this.cmdLeader);
        this.addSubCommand(this.cmdTitle);
        this.addSubCommand(this.cmdMap);
        this.addSubCommand(this.cmdSeeChunks);
        this.addSubCommand(this.cmdDisband);
        this.addSubCommand(this.cmdRelationAlly);
        this.addSubCommand(this.cmdRelationEnemy);
        this.addSubCommand(this.cmdRelationNeutral);
        this.addSubCommand(this.cmdRelationTruce);
        this.addSubCommand(this.cmdBypass);
        this.addSubCommand(this.cmdPowerBoost);
        this.addSubCommand(this.cmdPromote);
        this.addSubCommand(this.cmdLock);
        this.addSubCommand(this.cmdReload);
        this.addSubCommand(this.cmdConfig);
        this.addSubCommand(this.cmdSaveAll);
        this.addSubCommand(this.cmdVersion);
    }

    @Override
    public void perform() {
        this.commandChain.add(this);
        Main.main.cmdAutoHelp.execute(this.sender, this.args, this.commandChain);
    }

}
