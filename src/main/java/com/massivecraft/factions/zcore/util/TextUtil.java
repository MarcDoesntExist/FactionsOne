package com.massivecraft.factions.zcore.util;

import com.massivecraft.factions.util.Color;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text pipeline. Runtime text is parsed into Adventure {@link Component}s; the
 * legacy serializer is only used to read the existing configuration (color
 * names, tags.json values and chat formats), never to build messages.
 *
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class TextUtil {
    public static final Pattern patternTag = Pattern.compile("<([a-zA-Z0-9_]*)>");
    public static final long millisPerSecond = 1000;
    public static final long millisPerMinute = 60 * millisPerSecond;
    public static final long millisPerHour = 60 * millisPerMinute;
    public static final long millisPerDay = 24 * millisPerHour;
    public static final long millisPerWeek = 7 * millisPerDay;
    public static final long millisPerMonth = 31 * millisPerDay;
    public static final long millisPerYear = 365 * millisPerDay;

    private static final String titleizeLine = repeat("_", 52);
    private static final int titleizeBalance = -1;
    private static final String CONVERSION_CHARS = "sSdfxXoeEgGcCbBhH";

    private static final Map<String, Style> BUILT_IN_TAGS = new HashMap<>();
    private static final Map<Character, TextColor> LEGACY_COLORS = new HashMap<>();
    private static final Map<Character, TextDecoration> LEGACY_DECORATIONS = new HashMap<>();

    public static Map<String, Long> unitMillis = new LinkedHashMap<>();

    static {
        unitMillis.put("years", millisPerYear);
        unitMillis.put("months", millisPerMonth);
        unitMillis.put("weeks", millisPerWeek);
        unitMillis.put("days", millisPerDay);
        unitMillis.put("hours", millisPerHour);
        unitMillis.put("minutes", millisPerMinute);
        unitMillis.put("seconds", millisPerSecond);

        addBuiltInTag("black", NamedTextColor.BLACK);
        addBuiltInTag("navy", NamedTextColor.DARK_BLUE);
        addBuiltInTag("green", NamedTextColor.DARK_GREEN);
        addBuiltInTag("teal", NamedTextColor.DARK_AQUA);
        addBuiltInTag("red", NamedTextColor.DARK_RED);
        addBuiltInTag("purple", NamedTextColor.DARK_PURPLE);
        addBuiltInTag("gold", NamedTextColor.GOLD);
        addBuiltInTag("silver", NamedTextColor.GRAY);
        addBuiltInTag("gray", NamedTextColor.DARK_GRAY);
        addBuiltInTag("blue", NamedTextColor.BLUE);
        addBuiltInTag("lime", NamedTextColor.GREEN);
        addBuiltInTag("aqua", NamedTextColor.AQUA);
        addBuiltInTag("rose", NamedTextColor.RED);
        addBuiltInTag("pink", NamedTextColor.LIGHT_PURPLE);
        addBuiltInTag("yellow", NamedTextColor.YELLOW);
        addBuiltInTag("white", NamedTextColor.WHITE);

        LEGACY_COLORS.put('0', NamedTextColor.BLACK);
        LEGACY_COLORS.put('1', NamedTextColor.DARK_BLUE);
        LEGACY_COLORS.put('2', NamedTextColor.DARK_GREEN);
        LEGACY_COLORS.put('3', NamedTextColor.DARK_AQUA);
        LEGACY_COLORS.put('4', NamedTextColor.DARK_RED);
        LEGACY_COLORS.put('5', NamedTextColor.DARK_PURPLE);
        LEGACY_COLORS.put('6', NamedTextColor.GOLD);
        LEGACY_COLORS.put('7', NamedTextColor.GRAY);
        LEGACY_COLORS.put('8', NamedTextColor.DARK_GRAY);
        LEGACY_COLORS.put('9', NamedTextColor.BLUE);
        LEGACY_COLORS.put('a', NamedTextColor.GREEN);
        LEGACY_COLORS.put('b', NamedTextColor.AQUA);
        LEGACY_COLORS.put('c', NamedTextColor.RED);
        LEGACY_COLORS.put('d', NamedTextColor.LIGHT_PURPLE);
        LEGACY_COLORS.put('e', NamedTextColor.YELLOW);
        LEGACY_COLORS.put('f', NamedTextColor.WHITE);

        LEGACY_DECORATIONS.put('k', TextDecoration.OBFUSCATED);
        LEGACY_DECORATIONS.put('l', TextDecoration.BOLD);
        LEGACY_DECORATIONS.put('m', TextDecoration.STRIKETHROUGH);
        LEGACY_DECORATIONS.put('n', TextDecoration.UNDERLINED);
        LEGACY_DECORATIONS.put('o', TextDecoration.ITALIC);
    }

    /**
     * Legacy tag values, kept for the chat format parser (config).
     */
    public final Map<String, String> tags = new HashMap<>();
    /**
     * Runtime tag styles, built once from tags.json at startup.
     */
    public final Map<String, Style> tagStyles = new HashMap<>();

    private static void addBuiltInTag(String name, TextColor color) {
        BUILT_IN_TAGS.put(name, Style.style(color));
    }

    // -------------------------------------------- //
    // COMPONENT PARSING
    // -------------------------------------------- //

    public static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Converts a value from conf.json/tags.json into the style used by the parser.
     */
    public static Style styleFromConfig(String value) {
        if (value == null || value.isEmpty()) {
            return Style.empty();
        }
        String legacy = parseColor(value);
        if (legacy.isEmpty()) {
            return Style.empty();
        }
        return LegacyComponentSerializer.legacySection().deserialize(legacy).style();
    }

    public static String replaceTags(String str, Map<String, String> tags) {
        StringBuffer ret = new StringBuffer();
        Matcher matcher = patternTag.matcher(str);
        while (matcher.find()) {
            String tag = matcher.group(1);
            String repl = tags.get(tag);
            if (repl == null) {
                matcher.appendReplacement(ret, "<" + tag + ">");
            } else {
                matcher.appendReplacement(ret, repl);
            }
        }
        matcher.appendTail(ret);
        return ret.toString();
    }

    public static String parseColor(String string) {
        string = parseColorAmp(string);
        string = parseColorAcc(string);
        string = parseColorTags(string);
        return string;
    }

    // -------------------------------------------- //
    // CONFIG HELPERS (legacy is allowed here only)
    // -------------------------------------------- //

    public static String parseColorAmp(String string) {
        string = string.replaceAll("(\u00A7l([a-z0-9]))", "\u00A7$2");
        string = string.replaceAll("(&([a-z0-9]))", "\u00A7$2");
        string = string.replace("&&", "&");
        return string;
    }

    // -------------------------------------------- //
    // CONFIG STRING PARSING (no components)
    // -------------------------------------------- //

    public static String parseColorAcc(String string) {
        return string.replace("`e", "").replace("`r", Color.RED.toString())
            .replace("`R", Color.DARK_RED.toString()).replace("`y", Color.YELLOW.toString())
            .replace("`Y", Color.GOLD.toString()).replace("`g", Color.GREEN.toString())
            .replace("`G", Color.DARK_GREEN.toString()).replace("`a", Color.AQUA.toString())
            .replace("`A", Color.DARK_AQUA.toString()).replace("`b", Color.BLUE.toString())
            .replace("`B", Color.DARK_BLUE.toString())
            .replace("`main", Color.LIGHT_PURPLE.toString())
            .replace("`Main", Color.DARK_PURPLE.toString())
            .replace("`k", Color.BLACK.toString()).replace("`s", Color.GRAY.toString())
            .replace("`S", Color.DARK_GRAY.toString())
            .replace("`w", Color.WHITE.toString());
    }

    public static String parseColorTags(String string) {
        return string.replace("<empty>", "").replace("<black>", "\u00A70")
            .replace("<navy>", "\u00A71").replace("<green>", "\u00A72").replace("<teal>", "\u00A73")
            .replace("<red>", "\u00A74").replace("<purple>", "\u00A75").replace("<gold>", "\u00A76")
            .replace("<silver>", "\u00A77").replace("<gray>", "\u00A78")
            .replace("<blue>", "\u00A79").replace("<lime>", "\u00A7a").replace("<aqua>", "\u00A7b")
            .replace("<rose>", "\u00A7c").replace("<pink>", "\u00A7d")
            .replace("<yellow>", "\u00A7e").replace("<white>", "\u00A7f");
    }

    public static String upperCaseFirst(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    public static String repeat(String s, int times) {
        if (times <= 0) {
            return "";
        }
        return s + repeat(s, times - 1);
    }

    public static String implode(List<String> list, String glue) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                ret.append(glue);
            }
            ret.append(list.get(i));
        }
        return ret.toString();
    }

    /**
     * Joins components with a separator, keeping their styles.
     */
    public static Component implodeComponents(List<Component> list, Component glue) {
        Component ret = Component.empty();
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                ret = ret.append(glue);
            }
            ret = ret.append(list.get(i));
        }
        return ret;
    }

    // -------------------------------------------- //
    // TITLE / PAGES
    // -------------------------------------------- //

    public static String implodeCommaAnd(List<String> list, String comma, String and) {
        if (list.isEmpty()) {
            return "";
        }
        if (list.size() == 1) {
            return list.get(0);
        }

        String lastItem = list.get(list.size() - 1);
        String nextToLastItem = list.get(list.size() - 2);
        String merge = nextToLastItem + and + lastItem;
        list.set(list.size() - 2, merge);
        list.remove(list.size() - 1);

        return implode(list, comma);
    }

    public static String implodeCommaAnd(List<String> list) {
        return implodeCommaAnd(list, ", ", " and ");
    }

    public static String getMaterialName(Material material) {
        return material.toString().replace('_', ' ').toLowerCase();
    }

    // -------------------------------------------- //
    // STANDARD UTILS
    // -------------------------------------------- //

    public static String getTimeDeltaDescriptionRelNow(long millis) {
        String ret = "";

        double millisLeft = (double) Math.abs(millis);

        List<String> unitCountParts = new ArrayList<>();
        for (Entry<String, Long> entry : unitMillis.entrySet()) {
            if (unitCountParts.size() == 3) {
                break;
            }
            String unitName = entry.getKey();
            long unitSize = entry.getValue();
            long unitCount = (long) Math.floor(millisLeft / unitSize);
            if (unitCount < 1) {
                continue;
            }
            millisLeft -= unitSize * unitCount;
            unitCountParts.add(unitCount + " " + unitName);
        }

        if (unitCountParts.isEmpty()) {
            return "just now";
        }

        ret += implodeCommaAnd(unitCountParts);
        ret += " ";
        ret += millis <= 0 ? "ago" : "from now";
        return ret;
    }

    public static String getBestStartWithCI(Collection<String> candidates, String start) {
        String ret = null;
        int best = 0;

        start = start.toLowerCase();
        int minlength = start.length();
        for (String candidate : candidates) {
            if (candidate.length() < minlength || !candidate.toLowerCase().startsWith(start)) {
                continue;
            }

            int lendiff = candidate.length() - minlength;
            if (lendiff == 0) {
                return candidate;
            }
            if (lendiff < best || best == 0) {
                best = lendiff;
                ret = candidate;
            }
        }
        return ret;
    }

    private static Style applyStyle(Style current, Style next) {
        if (next.color() != null) {
            // A color resets the previous decorations, like a legacy color code.
            return next;
        }
        return current.decorations(next.decorations());
    }

    private static Style applyLegacyCode(Style current, char code) {
        char lower = Character.toLowerCase(code);
        if (lower == 'r') {
            return Style.empty();
        }
        TextColor color = LEGACY_COLORS.get(lower);
        if (color != null) {
            return Style.style(color);
        }
        TextDecoration decoration = LEGACY_DECORATIONS.get(lower);
        if (decoration != null) {
            return current.decoration(decoration, true);
        }
        return null;
    }

    private static int conversionEnd(String template, int start) {
        int index = start + 1;
        while (index < template.length()) {
            char c = template.charAt(index);
            if (c == '%') {
                return index;
            }
            if (Character.isLetter(c)) {
                return CONVERSION_CHARS.indexOf(c) >= 0 ? index : -1;
            }
            if ("0123456789$#+-.,( ".indexOf(c) < 0) {
                return -1;
            }
            index++;
        }
        return -1;
    }

    public Component parse(String str, Object... args) {
        if (str == null) {
            return Component.empty();
        }
        Renderer renderer = new Renderer(str, args);
        return renderer.render();
    }

    public Component parse(String str) {
        return parse(str, new Object[0]);
    }

    public List<Component> parseLines(Collection<String> lines) {
        List<Component> ret = new ArrayList<>(lines.size());
        for (String line : lines) {
            ret.add(parse(line));
        }
        return ret;
    }

    public String parseTags(String str) {
        return replaceTags(str, this.tags);
    }

    // -------------------------------------------- //
    // RENDERER
    // -------------------------------------------- //

    public Component titleize(String str) {
        return this.titleize(this.parse(str));
    }

    public Component titleize(Component content) {
        Style artStyle = this.getTagStyle("a");
        Style logoStyle = this.getTagStyle("l");
        Component title = Component.text("", logoStyle).append(content);
        Component center = Component.text(".[ ", artStyle)
            .append(title)
            .append(Component.text(" ]. ", artStyle));

        int centerlen = 7 + plain(content).length();
        int pivot = titleizeLine.length() / 2;
        int eatLeft = (centerlen / 2) - titleizeBalance;
        int eatRight = (centerlen - eatLeft) + titleizeBalance;

        if (eatLeft < pivot) {
            return Component.text(titleizeLine.substring(0, pivot - eatLeft), artStyle)
                .append(center)
                .append(Component.text(titleizeLine.substring(pivot + eatRight), artStyle));
        }
        return center;
    }

    public ArrayList<Component> getPage(List<Component> lines, int pageHumanBased, String title) {
        ArrayList<Component> ret = new ArrayList<>();
        int pageZeroBased = pageHumanBased - 1;
        int pageheight = 9;
        int pagecount = (lines.size() / pageheight) + 1;

        ret.add(this.titleize(title + " " + pageHumanBased + "/" + pagecount));

        if (pageZeroBased < 0 || pageHumanBased > pagecount) {
            ret.add(this.parse("<i>Invalid page. Must be between 1 and " + pagecount));
            return ret;
        }

        int from = pageZeroBased * pageheight;
        int to = Math.min(from + pageheight, lines.size());
        ret.addAll(lines.subList(from, to));
        return ret;
    }

    /**
     * @return the style configured for a tag, for building components that start with a tag.
     */
    public Style getTagStyle(String name) {
        Style style = this.resolveTagStyle(name);
        return style == null ? Style.empty() : style;
    }

    private Style resolveTagStyle(String name) {
        Style custom = this.tagStyles.get(name);
        if (custom != null) {
            return custom;
        }
        Style builtIn = BUILT_IN_TAGS.get(name);
        if (builtIn != null) {
            return builtIn;
        }
        if ("empty".equals(name)) {
            return Style.empty();
        }
        return null;
    }

    private final class Renderer {
        private final String template;
        private final Object[] args;
        private final StringBuilder literal = new StringBuilder();
        private int index;
        private int argIndex;
        private Style style = Style.empty();
        private Component result = Component.empty();

        private Renderer(String template, Object[] args) {
            this.template = template;
            this.args = args == null ? new Object[0] : args;
        }

        private Component render() {
            while (this.index < this.template.length()) {
                char c = this.template.charAt(this.index);

                if (c == '%' && this.renderPlaceholder()) {
                    continue;
                }

                if (c == '<' && this.renderTag()) {
                    continue;
                }

                if ((c == '\u00A7' || c == '&' || c == '`') && this.renderLegacyCode()) {
                    continue;
                }

                this.literal.append(c);
                this.index++;
            }
            this.flush();
            return this.result;
        }

        private boolean renderTag() {
            Matcher matcher = patternTag.matcher(this.template);
            matcher.region(this.index, this.template.length());
            if (!matcher.lookingAt()) {
                return false;
            }

            Style next = resolveTagStyle(matcher.group(1));
            if (next == null) {
                return false;
            }

            this.flush();
            this.style = applyStyle(this.style, next);
            this.index = matcher.end();
            return true;
        }

        private boolean renderLegacyCode() {
            char c = this.template.charAt(this.index);

            if (c == '&' && this.index + 1 < this.template.length()
                && this.template.charAt(this.index + 1) == '&') {
                this.literal.append('&');
                this.index += 2;
                return true;
            }

            if (c == '`' && this.template.startsWith("`main", this.index)) {
                this.flush();
                this.style = Style.style(NamedTextColor.LIGHT_PURPLE);
                this.index += 5;
                return true;
            }
            if (c == '`' && this.template.startsWith("`Main", this.index)) {
                this.flush();
                this.style = Style.style(NamedTextColor.DARK_PURPLE);
                this.index += 5;
                return true;
            }

            if (c == '`' && this.index + 1 < this.template.length()
                && "e".equals(String.valueOf(this.template.charAt(this.index + 1)))) {
                this.index += 2;
                return true;
            }

            if (this.index + 1 >= this.template.length()) {
                return false;
            }

            char code = this.template.charAt(this.index + 1);
            Style next = applyLegacyCode(this.style, code);
            if (next == null) {
                return false;
            }

            this.flush();
            this.style = next;
            this.index += 2;
            return true;
        }

        private boolean renderPlaceholder() {
            if (this.index + 1 >= this.template.length()) {
                return false;
            }
            if (this.template.charAt(this.index + 1) == '%') {
                this.literal.append('%');
                this.index += 2;
                return true;
            }

            int end = conversionEnd(this.template, this.index);
            if (end < 0) {
                return false;
            }

            String specifier = this.template.substring(this.index, end + 1);
            Object arg = this.argIndex < this.args.length ? this.args[this.argIndex++] : null;
            this.flush();

            if (arg instanceof Component) {
                this.result = this.result.append(((Component) arg).applyFallbackStyle(this.style));
            } else {
                String formatted;
                try {
                    formatted = String.format(specifier, arg);
                } catch (RuntimeException ex) {
                    formatted = String.valueOf(arg);
                }
                this.result = this.result.append(Component.text(formatted, this.style));
            }

            this.index = end + 1;
            return true;
        }

        private void flush() {
            if (this.literal.length() > 0) {
                this.result = this.result.append(Component.text(this.literal.toString(), this.style));
                this.literal.setLength(0);
            }
        }
    }
}
