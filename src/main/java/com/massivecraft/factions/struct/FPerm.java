package com.massivecraft.factions.struct;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.iface.RelationParticipator;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Permissions that you (a player) may or may not have in the territory of a certain faction.
 * Each faction have many Rel's assigned to each one of these Perms.
 *
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public enum FPerm {
    BUILD("build", "edit the terrain", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER,
        Rel.RECRUIT, Rel.ALLY),
    PAINBUILD("painbuild", "edit, take damage"),
    DOOR("door", "use doors", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER, Rel.RECRUIT,
        Rel.ALLY),
    BUTTON("button", "use stone buttons", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER,
        Rel.RECRUIT, Rel.ALLY),
    LEVER("lever", "use levers", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER, Rel.RECRUIT,
        Rel.ALLY),
    CONTAINER("container", "use containers", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER),
    INVITE("invite", "invite players", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    KICK("kick", "kick members", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    SETHOME("sethome", "set the home", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    WITHDRAW("withdraw", "withdraw money", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    TERRITORY("territory", "claim or unclaim", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    CAPE("cape", "set the cape", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    ACCESS("access", "grant territory", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    DISBAND("disband", "disband the faction", Rel.LEADER),
    PERMS("perms", "manage permissions", Rel.LEADER, Rel.COLEADER),
    WARP("warp", "use faction warps", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER),
    SETWARP("setwarp", "set faction warps", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    FLY("fly", "fly in faction territory", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER),
    BANK_DEPOSIT("bankdeposit", "deposit into the faction bank", Rel.LEADER, Rel.COLEADER,
        Rel.OFFICER, Rel.MEMBER, Rel.RECRUIT),
    BANK_WITHDRAW("bankwithdraw", "withdraw from the faction bank", Rel.LEADER, Rel.COLEADER,
        Rel.OFFICER),
    TNT_DEPOSIT("tntdeposit", "deposit TNT into the faction bank", Rel.LEADER, Rel.COLEADER,
        Rel.OFFICER, Rel.MEMBER, Rel.RECRUIT),
    TNT_WITHDRAW("tntwithdraw", "withdraw TNT from the faction bank", Rel.LEADER, Rel.COLEADER,
        Rel.OFFICER),
    UPGRADE("upgrade", "upgrade the faction", Rel.LEADER, Rel.COLEADER),
    SHOP("shop", "use the faction shop", Rel.LEADER, Rel.COLEADER, Rel.OFFICER, Rel.MEMBER,
        Rel.RECRUIT),
    MISSIONS("missions", "manage faction missions", Rel.LEADER, Rel.COLEADER, Rel.OFFICER,
        Rel.MEMBER, Rel.RECRUIT),
    CHAT_ALLY("chatally", "use the ally chat channel", Rel.LEADER, Rel.COLEADER, Rel.OFFICER,
        Rel.MEMBER, Rel.RECRUIT),
    LOGS("logs", "view faction logs", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    BAN("ban", "ban players from the faction", Rel.LEADER, Rel.COLEADER, Rel.OFFICER),
    MENU_MEMBERS("menumembers", "open the members menu", Rel.LEADER, Rel.COLEADER, Rel.OFFICER,
        Rel.MEMBER, Rel.RECRUIT),
    MENU_PERMS("menuperms", "open the permissions menu", Rel.LEADER, Rel.COLEADER),
    ;

    // Perms which apply strictly to granting territory access
    private static final Set<FPerm> TerritoryPerms =
        EnumSet.of(BUILD, DOOR, BUTTON, LEVER, CONTAINER);
    private static final String errorpattern = "%s<b> does not allow you to %s<b>.";
    public final Set<Rel> defaultDefaultValue;
    private final String nicename;
    private final String desc;

    FPerm(final String nicename, final String desc, final Rel... rels) {
        this.nicename = nicename;
        this.desc = desc;
        this.defaultDefaultValue = new HashSet<Rel>();
        this.defaultDefaultValue.addAll(Arrays.asList(rels));
    }

    public static FPerm parse(String str) {
        if (str == null) {
            return null;
        }

        String normalized = str.trim().toLowerCase(Locale.ROOT)
            .replace("_", "").replace("-", "").replace(".", "").replace(" ", "");
        if (normalized.isEmpty()) {
            return null;
        }

        for (FPerm perm : FPerm.values()) {
            String enumName = perm.name().toLowerCase(Locale.ROOT).replace("_", "");
            if (enumName.equals(normalized) || perm.nicename.equals(normalized)) {
                return perm;
            }
        }

        // Longest prefixes first so new permissions do not change the meaning of
        // the short forms used by existing setups.
        if (normalized.startsWith("wa"))
            return WARP;
        if (normalized.startsWith("w"))
            return WITHDRAW;
        if (normalized.startsWith("setw"))
            return SETWARP;
        if (normalized.startsWith("sh"))
            return SHOP;
        if (normalized.startsWith("s"))
            return SETHOME;
        if (normalized.startsWith("bankd"))
            return BANK_DEPOSIT;
        if (normalized.startsWith("bankw"))
            return BANK_WITHDRAW;
        if (normalized.startsWith("ban"))
            return BAN;
        if (normalized.startsWith("bui"))
            return BUILD;
        if (normalized.startsWith("but"))
            return BUTTON;
        if (normalized.startsWith("pa"))
            return PAINBUILD;
        if (normalized.startsWith("do"))
            return DOOR;
        if (normalized.startsWith("di"))
            return DISBAND;
        if (normalized.startsWith("lo"))
            return LOGS;
        if (normalized.startsWith("l"))
            return LEVER;
        if (normalized.startsWith("chat"))
            return CHAT_ALLY;
        if (normalized.startsWith("ca"))
            return CAPE;
        if (normalized.startsWith("co"))
            return CONTAINER;
        if (normalized.startsWith("i"))
            return INVITE;
        if (normalized.startsWith("k"))
            return KICK;
        if (normalized.startsWith("tntd"))
            return TNT_DEPOSIT;
        if (normalized.startsWith("tntw"))
            return TNT_WITHDRAW;
        if (normalized.startsWith("t"))
            return TERRITORY;
        if (normalized.startsWith("menum"))
            return MENU_MEMBERS;
        if (normalized.startsWith("menup"))
            return MENU_PERMS;
        if (normalized.startsWith("mi") || normalized.equals("m"))
            return MISSIONS;
        if (normalized.startsWith("u"))
            return UPGRADE;
        if (normalized.startsWith("f"))
            return FLY;
        if (normalized.startsWith("a"))
            return ACCESS;
        if (normalized.startsWith("pe"))
            return PERMS;
        return null;
    }

    public static String getStateHeaders() {
        String ret = "";
        for (Rel rel : Rel.values()) {
            ret += rel.getColor().toString();
            ret += rel.toString().substring(0, 3);
            ret += " ";
        }

        return ret;
    }

    public String getNicename() {
        return this.nicename;
    }

    public String getDescription() {
        return this.desc;
    }

    public Set<Rel> getDefault() {
        Set<Rel> ret = Conf.factionPermDefaults.get(this);
        if (ret == null)
            return this.defaultDefaultValue;
        return ret;
    }

    public String getStateInfo(Set<Rel> value, boolean withDesc) {
        String ret = "";

        for (Rel rel : Rel.values()) {
            if (value.contains(rel)) {
                ret += "<g>YES";
            } else {
                ret += "<b>NOO";
            }
            ret += " ";
        }

        ret += "<c>" + this.getNicename();
        if (withDesc) {
            ret += " <i>" + this.getDescription();
        }
        return ret;
    }

    public boolean isTerritoryPerm() {
        return TerritoryPerms.contains(this);
    }

    public boolean has(Object testSubject, Faction hostFaction, boolean informIfNot) {
        if (testSubject instanceof ConsoleCommandSender)
            return true;

        RelationParticipator rpSubject = null;

        if (testSubject instanceof Player) {
            rpSubject = FPlayers.i.get((Player) testSubject);
        } else if (testSubject instanceof RelationParticipator) {
            rpSubject = (RelationParticipator) testSubject;
        } else {
            return false;
        }

        Rel rel = rpSubject.getRelationTo(hostFaction);

        // TODO: Create better description messages like: "You must at least be officer".
        boolean ret = hostFaction.getPermittedRelations(this).contains(rel);

        if (rpSubject instanceof FPlayer && !ret && ((FPlayer) rpSubject).hasAdminMode())
            ret = true;

        if (!ret && informIfNot && rpSubject instanceof final FPlayer fplayer) {
            fplayer.msg(errorpattern, hostFaction.describeTo(fplayer, true), this.getDescription());
            if (Permission.ADMIN.has(fplayer.getPlayer())) {
                fplayer.msg("<i>You can bypass by using " + Main.main.cmdBase.cmdBypass
                    .getUseageTemplate(false));
            }
        }
        return ret;
    }

    public boolean has(Object testSubject, Faction hostFaction) {
        return this.has(testSubject, hostFaction, false);
    }

    public boolean has(Object testSubject, FLocation floc, boolean informIfNot) {
        TerritoryAccess access = Board.getTerritoryAccessAt(floc);
        if (this.isTerritoryPerm()) {
            if (access.subjectHasAccess(testSubject))
                return true;
            if (access.subjectAccessIsRestricted(testSubject)) {
                if (informIfNot) {
                    FPlayer notify = null;
                    if (testSubject instanceof Player)
                        notify = FPlayers.i.get((Player) testSubject);
                    else if (testSubject instanceof FPlayer)
                        notify = (FPlayer) testSubject;
                    if (notify != null)
                        notify
                            .msg("<b>This territory owned by your faction has restricted access.");
                }
                return false;
            }
        }
        return this.has(testSubject, access.getHostFaction(), informIfNot);
    }

    public boolean has(Object testSubject, Location loc, boolean informIfNot) {
        FLocation floc = new FLocation(loc);
        return this.has(testSubject, floc, informIfNot);
    }

    public boolean has(Object testSubject, Location loc) {
        return this.has(testSubject, loc, false);
    }

    public boolean has(Object testSubject, FLocation floc) {
        return this.has(testSubject, floc, false);
    }
}
