package com.massivecraft.factions.integration.map;

/**
 * A claimed chunk in a world, used by the map overlay without touching Bukkit.
 *
 * @author MarcDoesntExists
 */
public record ClaimKey(String world, int x, int z) {
}
