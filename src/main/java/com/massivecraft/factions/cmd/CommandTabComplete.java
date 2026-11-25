package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.Color;
import com.massivecraft.factions.zcore.CommandVisibility;
import com.massivecraft.factions.zcore.MCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Completes the existing {@link MCommand} tree without a second command model.
 * The same tree is used by the Brigadier bridge in {@code Main}, so execution
 * and suggestions cannot drift apart.
 */
public final class CommandTabComplete {

    private static final List<String> BOOLEAN_VALUES = List.of("yes", "no");
    private static final List<String> TOGGLE_VALUES = List.of("on", "off");
    private static final List<String> RELOAD_VALUES =
            List.of("all", "conf", "board", "factions", "players");

    private CommandTabComplete() {
    }

    public static List<String> complete(MCommand<?> root, CommandSender sender, List<String> rawArgs) {
        List<String> args = new ArrayList<>();
        for (int i = 0; i < rawArgs.size(); i++) {
            String arg = rawArgs.get(i);
            // Brigadier preserves repeated spaces in suggestions; empty middle
            // arguments are not meaningful for this command tree.
            if (!arg.isEmpty() || i == rawArgs.size() - 1) {
                args.add(arg);
            }
        }
        if (args.isEmpty()) {
            args.add("");
        }
        return completeCommand(root, sender, args);
    }

    private static List<String> completeCommand(MCommand<?> command, CommandSender sender,
                                                List<String> args) {
        String current = args.get(args.size() - 1);

        if (!command.subCommands.isEmpty()) {
            if (args.size() > 1) {
                MCommand<?> sub = findSubCommand(command, args.get(0));
                if (sub == null || !isVisible(sub, sender)) {
                    return Collections.emptyList();
                }
                return completeCommand(sub, sender, args.subList(1, args.size()));
            }
            return completeSubCommands(command, sender, current);
        }

        int index = args.size() - 1;
        String descriptor = getDescriptor(command, index);
        if (descriptor == null) {
            return Collections.emptyList();
        }
        return completeArgument(command, sender, descriptor, current);
    }

    private static MCommand<?> findSubCommand(MCommand<?> command, String token) {
        for (MCommand<?> sub : command.subCommands) {
            for (String alias : sub.aliases) {
                if (alias.equalsIgnoreCase(token)) {
                    return sub;
                }
            }
        }
        return null;
    }

    private static List<String> completeSubCommands(MCommand<?> command, CommandSender sender,
                                                    String current) {
        List<String> ret = new ArrayList<>();
        for (MCommand<?> sub : command.subCommands) {
            if (!isVisible(sub, sender)) {
                continue;
            }

            if (current.isEmpty()) {
                addIfMatching(ret, primaryAlias(sub), current);
            } else {
                for (String alias : sub.aliases) {
                    addIfMatching(ret, alias, current);
                }
            }
        }
        return sortedDistinct(ret);
    }

    private static boolean isVisible(MCommand<?> command, CommandSender sender) {
        if (command.visibility == CommandVisibility.INVISIBLE) {
            return false;
        }
        if (command.visibility == CommandVisibility.SECRET
                && !command.validSenderPermissions(sender, false)) {
            return false;
        }
        return !command.senderMustBePlayer || sender instanceof Player;
    }

    private static String primaryAlias(MCommand<?> command) {
        String ret = "";
        for (String alias : command.aliases) {
            if (ret.isEmpty() || alias.length() > ret.length()) {
                ret = alias;
            }
        }
        return ret;
    }

    private static String getDescriptor(MCommand<?> command, int index) {
        if (index < command.requiredArgs.size()) {
            return command.requiredArgs.get(index);
        }

        int optionalIndex = index - command.requiredArgs.size();
        if (optionalIndex >= command.optionalArgs.size()) {
            return null;
        }

        int current = 0;
        for (String descriptor : command.optionalArgs.keySet()) {
            if (current == optionalIndex) {
                return descriptor;
            }
            current++;
        }
        return null;
    }

    private static List<String> completeArgument(MCommand<?> command, CommandSender sender,
                                                 String rawDescriptor, String current) {
        String descriptor = rawDescriptor.toLowerCase(Locale.ROOT);

        if (descriptor.contains("|")) {
            return filter(Arrays.asList(descriptor.split("\\|")), current);
        }

        return switch (descriptor) {
            case "player", "player name" -> playerNames(sender, current);
            case "faction" -> factionTags(current);
            case "flag" -> flagNames(current);
            case "perm" -> permNames(current);
            case "relation" -> relationNames(current);
            case "name" -> merge(playerNames(sender, current), factionTags(current));
            case "yes/no" -> filter(BOOLEAN_VALUES, current);
            case "on/off" -> filter(TOGGLE_VALUES, current);
            case "page" -> pages(current);
            case "file" -> filter(RELOAD_VALUES, current);
            case "setting" -> configKeys(current);
            default -> Collections.emptyList();
        };
    }

    private static List<String> playerNames(CommandSender sender, String current) {
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
        return filter(names, current);
    }

    private static List<String> factionTags(String current) {
        Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Faction faction : Factions.i.get()) {
            String tag = Color.strip(faction.getTagPlain());
            if (tag != null && !tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return filter(tags, current);
    }

    private static List<String> flagNames(String current) {
        List<String> values = new ArrayList<>();
        for (FFlag flag : FFlag.values()) {
            values.add(flag.getNicename());
        }
        return filter(values, current);
    }

    private static List<String> permNames(String current) {
        List<String> values = new ArrayList<>();
        for (FPerm perm : FPerm.values()) {
            values.add(perm.getNicename());
        }
        return filter(values, current);
    }

    private static List<String> relationNames(String current) {
        List<String> values = new ArrayList<>();
        for (Rel rel : Rel.values()) {
            values.add(rel.name().toLowerCase(Locale.ROOT));
        }
        return filter(values, current);
    }

    private static List<String> pages(String current) {
        List<String> values = new ArrayList<>();
        int max = Math.max(5, (Factions.i.get().size() / 9) + 1);
        for (int page = 1; page <= max; page++) {
            values.add(Integer.toString(page));
        }
        return filter(values, current);
    }

    private static List<String> configKeys(String current) {
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Field field : Conf.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())
                    && Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                values.add(field.getName());
            }
        }
        return filter(values, current);
    }

    private static List<String> merge(Collection<String> first, Collection<String> second) {
        Set<String> values = new LinkedHashSet<>(first);
        values.addAll(second);
        return sortedDistinct(values);
    }

    private static List<String> filter(Collection<String> values, String current) {
        List<String> ret = new ArrayList<>();
        String prefix = current.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                ret.add(value);
            }
        }
        return sortedDistinct(ret);
    }

    private static void addIfMatching(List<String> values, String value, String current) {
        if (value != null && value.toLowerCase(Locale.ROOT)
                .startsWith(current.toLowerCase(Locale.ROOT))) {
            values.add(value);
        }
    }

    private static List<String> sortedDistinct(Collection<String> values) {
        Set<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll(values);
        return new ArrayList<>(sorted);
    }
}
