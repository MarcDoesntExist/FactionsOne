package com.massivecraft.factions.integration.map;

/**
 * A rectangular group of chunks in one world. Bounds are inclusive chunk
 * coordinates.
 *
 * @author MarcDoesntExists
 */
public record ClaimRect(String world, int minX, int minZ, int maxX, int maxZ) {
}
