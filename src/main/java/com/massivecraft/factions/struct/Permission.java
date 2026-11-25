package com.massivecraft.factions.struct;

import com.massivecraft.factions.Main;
import org.bukkit.command.CommandSender;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public enum Permission {
    ACCESS("access"), ACCESS_ANY("access.any"), ACCESS_VIEW("access.view"), ADMIN(
            "adminmode"), AUTOCLAIM("autoclaim"), BAN("ban"), BAN_LIST("banlist"), CAPE("cape"),
    CAPE_GET("cape.get"), CAPE_SET(
            "cape.set"), CAPE_REMOVE("cape.remove"), CLAIM("claim"), CLAIM_RADIUS(
            "claim.radius"), COORDS("coords"), CONFIG("config"), CREATE("create"), DEINVITE("deinvite"), DEMOTE(
            "demote"), DESCRIPTION("description"), DISBAND("disband"), FLAG("flag"), FLAG_SET(
            "flag.set"), HELP("help"), HOME("home"), INVITE("invite"), JOIN("join"), JOIN_ANY(
            "join.any"), JOIN_OTHERS("join.others"), KICK("kick"), LEADER("leader"), LEADER_ANY(
            "leader.any"), LEAVE("leave"), LIST("list"), LOCK("lock"), MAIL("mail"), MAIL_SEND("mail.send"), MAP("map"), MONEY_BALANCE(
            "money.balance"), MONEY_BALANCE_ANY("money.balance.any"), MONEY_DEPOSIT(
            "money.deposit"), MONEY_F2F("money.f2f"), MONEY_F2P("money.f2p"), MONEY_P2F(
            "money.p2f"),     MONEY_WITHDRAW(    "money.withdraw"), MENU("menu"), CHAT("chat"), FLY("fly"), LOGS("logs"), MISSIONS("missions"), OFFICER("officer"), OFFICER_ANY(
            "officer.any"), OPEN("open"), PERM("perm"), NEAR("near"), POWER("power"), POWER_ANY(
            "power.any"), POWERBOOST("powerboost"), PROMOTE("promote"), RELATION("relation"), RELOAD(
            "reload"), SAVE("save"), SEE_CHUNK("seechunk"), SETHOME("sethome"), SETWARP("setwarp"), SHIELD("shield"), SHIELD_SET("shield.set"), SHOW("show"), SHOP("shop"), TAG(
            "tag"), TAX("tax"), TAX_RUN("tax.run"), TITLE("title"), TNT("tnt"), TOP("top"), STUCK("stuck"), STATUS("status"), UNBAN("unban"), UNCLAIM("unclaim"), UNCLAIM_ALL("unclaimall"), UPGRADE("upgrade"), VERSION("version"), WARP("warp"),
    ;

    public final String node;

    Permission(final String node) {
        this.node = "factions." + node;
    }

    public boolean has(CommandSender sender, boolean informSenderIfNot) {
        return Main.main.perm.has(sender, this.node, informSenderIfNot);
    }

    public boolean has(CommandSender sender) {
        return has(sender, false);
    }
}
