package com.massivecraft.factions.integration.map;

/**
 * One rendered claim rectangle: a label and an RGB color.
 *
 * @author MarcDoesntExists
 */
public record MapClaim(String world, int minX, int minZ, int maxX, int maxZ,
                       String label, int rgb) {
}
