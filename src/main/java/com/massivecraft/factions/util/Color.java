package com.massivecraft.factions.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.regex.Pattern;

/**
 * Legacy chat color codes used by the plugin's string based text pipeline
 * (config, faction tags, map rendering and chat tags).
 *
 * <p>Replaces {@code org.bukkit.ChatColor}, which is deprecated on Paper. The
 * constant names match the old enum, so existing {@code conf.json} values keep
 * working unchanged.</p>
 *
 * @author MarcDoesntExists
 */
public enum Color {
    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f'),
    MAGIC('k'),
    BOLD('l'),
    STRIKETHROUGH('m'),
    UNDERLINE('n'),
    ITALIC('o'),
    RESET('r');

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");

    private final String code;

    Color(char c) {
        this.code = "\u00A7" + c;
    }

    /**
     * Removes all legacy color codes from a string.
     *
     * @param input the string to clean
     * @return the input without color codes, or {@code null} if the input was null
     */
    public static String strip(String input) {
        if (input == null) {
            return null;
        }
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * @return the legacy section-sign color code, e.g. {@code "§a"}
     */
    @Override
    public String toString() {
        return this.code;
    }

    /**
     * @return the Adventure color for this code, or {@code null} for formatting codes
     */
    public TextColor toTextColor() {
        return switch (this) {
            case BLACK -> NamedTextColor.BLACK;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case RED -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE -> NamedTextColor.WHITE;
            default -> null;
        };
    }

    /**
     * @return the Adventure decoration for this code, or {@code null} for colors
     */
    public TextDecoration toDecoration() {
        return switch (this) {
            case MAGIC -> TextDecoration.OBFUSCATED;
            case BOLD -> TextDecoration.BOLD;
            case STRIKETHROUGH -> TextDecoration.STRIKETHROUGH;
            case UNDERLINE -> TextDecoration.UNDERLINED;
            case ITALIC -> TextDecoration.ITALIC;
            default -> null;
        };
    }

}
