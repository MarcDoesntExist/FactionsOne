package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.Color;
import com.massivecraft.factions.zcore.MCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public abstract class FCommand extends MCommand<Main> {
    private static final List<String> BOOLEAN_VALUES = List.of("yes", "no");
    private static final List<String> TOGGLE_VALUES = List.of("on", "off");
    private static final List<String> RELOAD_VALUES =
        List.of("all", "conf", "board", "factions", "players");

    public boolean disableOnLock;

    public FPlayer fme;
    public Faction myFaction;
    public boolean senderMustBeMember;
    public boolean senderMustBeOfficer;
    public boolean senderMustBeLeader;

    public boolean isMoneyCommand;

    public FCommand() {
        super(Main.main);

        // Due to safety reasons it defaults to disable on lock.
        disableOnLock = true;

        // The money commands must be disabled if money should not be used.
        isMoneyCommand = false;

        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void execute(CommandSender sender, List<String> args, List<MCommand<?>> commandChain) {
        if (sender instanceof Player) {
            this.fme = FPlayers.i.get((Player) sender);
            this.myFaction = this.fme.getFaction();
        } else {
            this.fme = null;
            this.myFaction = null;
        }
        super.execute(sender, args, commandChain);
    }

    @Override
    public boolean isEnabled() {
        if (p.getLocked() && this.disableOnLock) {
            msg("<b>Factions was locked by an admin. Please try again later.");
            return false;
        }

        if (this.isMoneyCommand && !Conf.econEnabled) {
            msg("<b>Faction economy features are disabled on this server.");
            return false;
        }

        if (this.isMoneyCommand && !Conf.bankEnabled) {
            msg("<b>The faction bank system is disabled on this server.");
            return false;
        }

        return true;
    }

    @Override
    public boolean validSenderType(CommandSender sender, boolean informSenderIfNot) {
        boolean superValid = super.validSenderType(sender, informSenderIfNot);
        if (!superValid)
            return false;

        if (!(this.senderMustBeMember || this.senderMustBeOfficer || this.senderMustBeLeader))
            return true;

        if (!(sender instanceof Player))
            return false;

        FPlayer fplayer = FPlayers.i.get((Player) sender);

        if (!fplayer.hasFaction()) {
            sender.sendMessage(p.txt.parse("<b>You are not member of any faction."));
            return false;
        }

        if (this.senderMustBeOfficer && !fplayer.getRole().isAtLeast(Rel.OFFICER)) {
            sender.sendMessage(
                p.txt.parse("<b>Only faction moderators can %s.", this.getHelpShort()));
            return false;
        }

        if (this.senderMustBeLeader && !fplayer.getRole().isAtLeast(Rel.LEADER)) {
            sender.sendMessage(p.txt.parse("<b>Only faction admins can %s.", this.getHelpShort()));
            return false;
        }

        return true;
    }

    // -------------------------------------------- //
    // Assertions
    // -------------------------------------------- //

    public boolean assertHasFaction() {
        if (me == null)
            return true;

        if (!fme.hasFaction()) {
            sendMessage("You are not member of any faction.");
            return false;
        }
        return true;
    }

    public boolean assertMinRole(Rel role) {
        if (me == null)
            return true;

        if (fme.getRole().isLessThan(role)) {
            msg("<b>You <h>must be " + role + "<b> to " + this.getHelpShort() + ".");
            return false;
        }
        return true;
    }

    // -------------------------------------------- //
    // Argument Readers
    // -------------------------------------------- //

    // FPLAYER ======================
    public FPlayer strAsFPlayer(String name, FPlayer def, boolean msg) {
        FPlayer ret = def;

        if (name != null) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            FPlayer fplayer = FPlayers.i.get(player);
            if (fplayer != null) {
                ret = fplayer;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>No player \"<main>%s<b>\" could be found.", name);
        }

        return ret;
    }

    public FPlayer argAsFPlayer(int idx, FPlayer def, boolean msg) {
        return this.strAsFPlayer(this.argAsString(idx), def, msg);
    }

    public FPlayer argAsFPlayer(int idx, FPlayer def) {
        return this.argAsFPlayer(idx, def, true);
    }

    public FPlayer argAsFPlayer(int idx) {
        return this.argAsFPlayer(idx, null);
    }

    // BEST FPLAYER MATCH ======================
    public FPlayer strAsBestFPlayerMatch(String name, FPlayer def, boolean msg) {
        return strAsFPlayer(name, def, msg);
    }

    public FPlayer argAsBestFPlayerMatch(int idx, FPlayer def, boolean msg) {
        return this.strAsBestFPlayerMatch(this.argAsString(idx), def, msg);
    }

    public FPlayer argAsBestFPlayerMatch(int idx, FPlayer def) {
        return this.argAsBestFPlayerMatch(idx, def, true);
    }

    public FPlayer argAsBestFPlayerMatch(int idx) {
        return this.argAsBestFPlayerMatch(idx, null);
    }

    // FACTION ======================
    public Faction strAsFaction(String name, Faction def, boolean msg) {
        Faction ret = def;

        if (name != null) {
            Faction faction = null;

            // First we try an exact match
            if (faction == null) {
                faction = Factions.i.getByTag(name);
            }

            // Next we match faction tags
            if (faction == null) {
                faction = Factions.i.getBestTagMatch(name);
            }

            // Next we match player names
            if (faction == null) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(name);
                FPlayer fplayer = FPlayers.i.get(player);
                if (fplayer != null) {
                    faction = fplayer.getFaction();

                }
            }

            if (faction != null) {
                ret = faction;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>The faction or player \"<main>%s<b>\" could not be found.", name);
        }

        return ret;
    }

    public Faction argAsFaction(int idx, Faction def, boolean msg) {
        return this.strAsFaction(this.argAsString(idx), def, msg);
    }

    public Faction argAsFaction(int idx, Faction def) {
        return this.argAsFaction(idx, def, true);
    }

    public Faction argAsFaction(int idx) {
        return this.argAsFaction(idx, null);
    }

    // FACTION FLAG ======================
    public FFlag strAsFactionFlag(String name, FFlag def, boolean msg) {
        FFlag ret = def;

        if (name != null) {
            FFlag flag = FFlag.parse(name);
            if (flag != null) {
                ret = flag;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>The faction-flag \"<main>%s<b>\" could not be found.", name);
        }

        return ret;
    }

    public FFlag argAsFactionFlag(int idx, FFlag def, boolean msg) {
        return this.strAsFactionFlag(this.argAsString(idx), def, msg);
    }

    public FFlag argAsFactionFlag(int idx, FFlag def) {
        return this.argAsFactionFlag(idx, def, true);
    }

    public FFlag argAsFactionFlag(int idx) {
        return this.argAsFactionFlag(idx, null);
    }

    // FACTION PERM ======================
    public FPerm strAsFactionPerm(String name, FPerm def, boolean msg) {
        FPerm ret = def;

        if (name != null) {
            FPerm perm = FPerm.parse(name);
            if (perm != null) {
                ret = perm;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>The faction-perm \"<main>%s<b>\" could not be found.", name);
        }

        return ret;
    }

    public FPerm argAsFactionPerm(int idx, FPerm def, boolean msg) {
        return this.strAsFactionPerm(this.argAsString(idx), def, msg);
    }

    public FPerm argAsFactionPerm(int idx, FPerm def) {
        return this.argAsFactionPerm(idx, def, true);
    }

    public FPerm argAsFactionPerm(int idx) {
        return this.argAsFactionPerm(idx, null);
    }

    // FACTION REL ======================
    public Rel strAsRel(String name, Rel def, boolean msg) {
        Rel ret = def;

        if (name != null) {
            Rel perm = Rel.parse(name);
            if (perm != null) {
                ret = perm;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>The role \"<main>%s<b>\" could not be found.", name);
        }

        return ret;
    }

    public Rel argAsRel(int idx, Rel def, boolean msg) {
        return this.strAsRel(this.argAsString(idx), def, msg);
    }

    public Rel argAsRel(int idx, Rel def) {
        return this.argAsRel(idx, def, true);
    }

    public Rel argAsRel(int idx) {
        return this.argAsRel(idx, null);
    }

    // -------------------------------------------- //
    // Tab completion suggestions
    // -------------------------------------------- //

    @Override
    protected List<String> suggestArgument(CommandSender sender, String descriptor,
                                           String current) {
        String key = descriptor.toLowerCase(Locale.ROOT);
        if (key.contains("|")) {
            return filterStartingWith(Arrays.asList(key.split("\\|")), current);
        }

        return switch (key) {
            case "player", "player name" -> filterStartingWith(playerNames(sender), current);
            case "faction" -> filterStartingWith(factionTags(), current);
            case "flag" -> filterStartingWith(flagNames(), current);
            case "perm" -> filterStartingWith(permNames(), current);
            case "relation" -> filterStartingWith(relationNames(), current);
            case "name" -> filterStartingWith(nameAndFactionNames(sender), current);
            case "page" -> filterStartingWith(pageNumbers(), current);
            case "yes/no" -> filterStartingWith(BOOLEAN_VALUES, current);
            case "on/off" -> filterStartingWith(TOGGLE_VALUES, current);
            case "file" -> filterStartingWith(RELOAD_VALUES, current);
            case "setting" -> filterStartingWith(configKeys(), current);
            default -> Collections.emptyList();
        };
    }

    private List<String> playerNames(CommandSender sender) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player viewer && !viewer.canSee(player)) {
                continue;
            }
            names.add(player.getName());
        }

        if (sender instanceof Player player) {
            FPlayer fplayer = FPlayers.i.get(player);
            if (fplayer != null && fplayer.hasFaction()) {
                for (FPlayer member : fplayer.getFaction().getFPlayers()) {
                    names.add(member.getName());
                }
            }
        }
        return new ArrayList<>(names);
    }

    private List<String> factionTags() {
        Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Faction faction : Factions.i.get()) {
            String tag = Color.strip(faction.getTagPlain());
            if (tag != null && !tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return new ArrayList<>(tags);
    }

    private List<String> nameAndFactionNames(CommandSender sender) {
        List<String> names = new ArrayList<>(playerNames(sender));
        names.addAll(factionTags());
        return names;
    }

    private List<String> flagNames() {
        List<String> values = new ArrayList<>();
        for (FFlag flag : FFlag.values()) {
            values.add(flag.getNicename());
        }
        return values;
    }

    private List<String> permNames() {
        List<String> values = new ArrayList<>();
        for (FPerm perm : FPerm.values()) {
            values.add(perm.getNicename());
        }
        return values;
    }

    private List<String> relationNames() {
        List<String> values = new ArrayList<>();
        for (Rel rel : Rel.values()) {
            values.add(rel.name().toLowerCase(Locale.ROOT));
        }
        return values;
    }

    private List<String> pageNumbers() {
        // Root help has at least five pages; the faction list grows with the
        // number of factions, nine entries per page.
        int pages = Math.max(5, (Factions.i.get().size() / 9) + 1);
        List<String> values = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            values.add(Integer.toString(page));
        }
        return values;
    }

    private List<String> configKeys() {
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Field field : Conf.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())
                && Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                values.add(field.getName());
            }
        }
        return new ArrayList<>(values);
    }

    // -------------------------------------------- //
    // Commonly used logic
    // -------------------------------------------- //

    public boolean canIAdministerYou(FPlayer i, FPlayer you) {
        if (!i.getFaction().equals(you.getFaction())) {
            i.sendMessage(
                p.txt.parse("%s <b>is not in the same faction as you.", you.describeTo(i, true)));
            return false;
        }

        if (i.getRole().isMoreThan(you.getRole()) || i.getRole().equals(Rel.LEADER)) {
            return true;
        }

        if (you.getRole().isAtLeast(Rel.COLEADER)) {
            i.sendMessage(p.txt.parse("<b>Only the faction leader can do that."));
        } else if (i.getRole().equals(Rel.OFFICER)) {
            if (i == you) {
                return true; //Moderators can control themselves
            } else {
                i.sendMessage(p.txt.parse("<b>Moderators can't control each other..."));
            }
        } else {
            i.sendMessage(p.txt.parse("<b>You must be a faction moderator to do that."));
        }

        return false;
    }

    /**
     * Name of the acting player, or "console" for server senders.
     */
    protected String actorName() {
        return this.fme == null ? "console" : this.fme.getName();
    }

    // if economy is enabled and they're not on the bypass list, make 'em pay; returns true unless person can't afford the cost
    public boolean payForCommand(double cost, String toDoThis, String forDoingThis) {
        if (!Econ.shouldBeUsed() || this.fme == null || cost == 0.0 || fme.hasAdminMode())
            return true;

        if (Conf.bankEnabled && Conf.bankFactionPaysCosts && fme.hasFaction())
            return Econ.modifyMoney(myFaction, -cost, toDoThis, forDoingThis);
        else
            return Econ.modifyMoney(fme, -cost, toDoThis, forDoingThis);
    }

    // like above, but just make sure they can pay; returns true unless person can't afford the cost
    public boolean canAffordCommand(double cost, String toDoThis) {
        if (!Econ.shouldBeUsed() || this.fme == null || cost == 0.0 || fme.hasAdminMode())
            return true;

        if (Conf.bankEnabled && Conf.bankFactionPaysCosts && fme.hasFaction())
            return Econ.hasAtLeast(myFaction, cost, toDoThis);
        else
            return Econ.hasAtLeast(fme, cost, toDoThis);
    }
}
