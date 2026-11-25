package com.massivecraft.factions;

/**
 * Chat channels available to faction members.
 *
 * @author MarcDoesntExists
 */
public enum ChatChannel {
    PUBLIC,
    FACTION,
    ALLY;

    public static ChatChannel parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
