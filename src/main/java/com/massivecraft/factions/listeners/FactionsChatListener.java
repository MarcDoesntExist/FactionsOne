package com.massivecraft.factions.listeners;

import com.massivecraft.factions.ChatChannel;
import com.massivecraft.factions.ChatChannelResolver;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import com.massivecraft.factions.Faction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FactionsChatListener implements Listener {
    public static final Pattern parsePattern =
            Pattern.compile("[{\\[]factions?_([a-zA-Z_]+)[}\\]]");

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();
    private static final Pattern FORMAT_PLACEHOLDER = Pattern.compile("%([0-9]+[$])?s");

    public Main main;

    public FactionsChatListener(Main main) {
        this.main = main;
    }

    /**
     * Sends the private channel copy to spies that were not normal recipients.
     * This is the listener's only role in the channel feature; channel delivery
     * itself is done by {@link com.massivecraft.factions.ChatChannelService}.
     */
    public static int spy(FPlayer sender, ChatChannel channel, String message) {
        if (sender == null || channel == null || channel == ChatChannel.PUBLIC) {
            return 0;
        }
        Faction faction = sender.getFaction();
        if (faction == null) {
            return 0;
        }
        int spies = 0;
        for (FPlayer other : FPlayers.i.getOnline()) {
            Player player = other.getPlayer();
            if (player == null || !Permission.CHAT_SPY.has(player, false)) {
                continue;
            }
            Faction otherFaction = other.getFaction();
            String relation = otherFaction == null
                    ? null
                    : faction.getRelationTo(otherFaction).name();
            if (ChatChannelResolver.accepts(channel, faction.getId(),
                    otherFaction == null ? null : otherFaction.getId(), relation)) {
                continue;
            }
            other.msg(Conf.chatSpyFormat, channel.name().toLowerCase(java.util.Locale.ROOT),
                    sender.getName(), message);
            spies++;
        }
        return spies;
    }

    // This is for handling insertion of the player's faction tag, set at highest priority to give
    // other plugins a chance to modify chat first.

    public static String parseTags(String str, Player from) {
        FPlayer fpfrom = FPlayers.i.get(from);
        return parseTags(str, from, fpfrom, null, null);
    }

    public static String parseTags(String str, Player from, FPlayer fpfrom) {
        return parseTags(str, from, fpfrom, null, null);
    }

    public static String parseTags(String str, Player from, Player to) {
        FPlayer fpfrom = FPlayers.i.get(from);
        FPlayer fpto = FPlayers.i.get(to);
        return parseTags(str, from, fpfrom, to, fpto);
    }

    public static String parseTags(String str, Player from, FPlayer fpfrom, Player to,
                                   FPlayer fpto) {
        StringBuffer ret = new StringBuffer();

        Matcher matcher = parsePattern.matcher(str);
        while (matcher.find()) {
            String[] parts = matcher.group(1).toLowerCase().split("_");
            List<String> args = new ArrayList<String>(Arrays.asList(parts));
            String tag = args.remove(0);
            matcher.appendReplacement(ret,
                    Matcher.quoteReplacement(produceTag(tag, args, from, fpfrom, to, fpto)));
        }
        matcher.appendTail(ret);

        return ret.toString();
    }

    public static String produceTag(String tag, List<String> args, Player from, FPlayer fpfrom,
                                    Player to, FPlayer fpto) {
        String ret = "";
        if (tag.equals("relcolor")) {
            if (fpto == null) {
                ret = Rel.NEUTRAL.getColor().toString();
            } else {
                ret = fpfrom.getRelationTo(fpto).getColor().toString();
            }
        } else if (tag.startsWith("roleprefix")) {
            ret = fpfrom.getRole().getPrefix();
        } else if (tag.equals("title")) {
            ret = fpfrom.getTitle();
        } else if (tag.equals("tag")) {
            if (fpfrom.hasFaction()) {
                ret = fpfrom.getFaction().getTagPlain();
            }
        } else if (tag.startsWith("tagforce") && fpfrom.hasFaction()) {
            ret = fpfrom.getFaction().getTagPlain();
        }

        if (ret == null)
            ret = "";

        return applyFormatsByName(ret, args);
    }

    public static String applyFormatsByName(String str, List<String> formatNames) {
        if (str.length() == 0)
            return str;
        for (String formatName : formatNames) {
            String format = Conf.chatSingleFormats.get(formatName);
            try {
                str = String.format(format, str);
            } catch (Exception e) {
            }
        }
        return str;
    }

    /**
     * Applies Factions' configured format without cancelling the event or sending messages
     * manually. Paper calls the renderer once per viewer, so the relation placeholder can use a
     * different color for every recipient while the signed chat event remains intact.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        // With no full format there is no Factions format string to parse. Leave the event alone
        // so another chat plugin can own the layout.
        if (!Conf.chatSetFormat)
            return;

        ChatRenderer previous = event.renderer();
        boolean composeWithPrevious = previous != ChatRenderer.defaultRenderer();

        event.renderer(new FactionsRenderer(
                previous,
                composeWithPrevious,
                Conf.chatSetFormatTo,
                Conf.chatParseTags && !Conf.chatTagHandledByAnotherPlugin,
                Conf.chatParseTagsColored));
    }

    private static final class FactionsRenderer implements ChatRenderer {
        private final ChatRenderer previous;
        private final boolean composeWithPrevious;
        private final String format;
        private final boolean parsePlaceholders;
        private final boolean relationColors;

        private FactionsRenderer(ChatRenderer previous, boolean composeWithPrevious, String format,
                                 boolean parsePlaceholders, boolean relationColors) {
            this.previous = previous;
            this.composeWithPrevious = composeWithPrevious;
            this.format = format;
            this.parsePlaceholders = parsePlaceholders;
            this.relationColors = relationColors;
        }

        private static String formatPrefix(String format) {
            try {
                return String.format(format, "", "");
            } catch (RuntimeException ignored) {
                return "";
            }
        }

        private static Component deserialize(String legacy) {
            return LEGACY.deserialize(legacy);
        }

        /**
         * Builds the configured layout as components. The legacy serializer only parses the
         * configured format string; display name and message are already components.
         * Supports both %s and positional %1$s/%2$s placeholders.
         */
        private static Component renderFormat(String format, Component displayName,
                                               Component message) {
            Matcher matcher = FORMAT_PLACEHOLDER.matcher(format);
            Component ret = Component.empty();
            int last = 0;
            int next = 0;

            while (matcher.find()) {
                ret = ret.append(deserialize(format.substring(last, matcher.start())));
                int arg;
                if (matcher.group(1) != null) {
                    String index = matcher.group(1);
                    arg = Integer.parseInt(index.substring(0, index.length() - 1)) - 1;
                } else {
                    arg = next++;
                }
                if (arg == 0) {
                    ret = ret.append(displayName);
                } else if (arg == 1) {
                    ret = ret.append(message);
                }
                last = matcher.end();
            }

            return ret.append(deserialize(format.substring(last)));
        }

        @Override
        public Component render(Player source, Component sourceDisplayName, Component message,
                                Audience viewer) {
            Player recipient = viewer instanceof Player ? (Player) viewer : null;
            FPlayer fpfrom = FPlayers.i.get(source);
            FPlayer fpto = relationColors && recipient != null
                    ? FPlayers.i.get(recipient)
                    : null;

            String resolved = parsePlaceholders
                    ? parseTags(format, source, fpfrom, recipient, fpto)
                    : format;

            if (composeWithPrevious) {
                // Another chat plugin already owns the layout. Keep its renderer and prepend the
                // Factions tag (the configured format with both %s arguments left empty), so the
                // two plugins don't overwrite each other.
                Component prefix = deserialize(formatPrefix(resolved));
                return prefix.append(previous.render(source, sourceDisplayName, message, viewer));
            }

            return renderFormat(resolved, sourceDisplayName, message);
        }
    }
}
