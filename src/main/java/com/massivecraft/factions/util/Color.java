package com.massivecraft.factions.util;

import java.util.regex.Pattern;

/**
 * Legacy chat color codes used by the plugin's string based text pipeline
 * (config, faction tags, map rendering and chat tags).
 *
 * <p>Replaces {@code org.bukkit.Color}, which is deprecated on Paper. The
 * constant names match the old enum, so existing {@code conf.json} values keep
 * working unchanged.</p>
 *
 * @author MarcDoesntExist
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
     * @return the legacy section-sign color code, e.g. {@code "§a"}
     */
    @Override
    public String toString() {
        return this.code;
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

}
