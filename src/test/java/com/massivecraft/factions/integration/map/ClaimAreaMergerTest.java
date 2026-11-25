package com.massivecraft.factions.integration.map;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless coverage for the map claim rectangle merger.
 *
 * @author MarcDoesntExists
 */
class ClaimAreaMergerTest {

    @Test
    void adjacentChunksBecomeOneRectangle() {
        List<ClaimRect> rects = ClaimAreaMerger.merge(List.of(
                new ClaimKey("world", 0, 0),
                new ClaimKey("world", 1, 0),
                new ClaimKey("world", 0, 1),
                new ClaimKey("world", 1, 1)));

        assertEquals(List.of(new ClaimRect("world", 0, 0, 1, 1)), rects);
    }

    @Test
    void gapsStaySeparate() {
        List<ClaimRect> rects = ClaimAreaMerger.merge(List.of(
                new ClaimKey("world", 0, 0),
                new ClaimKey("world", 2, 0)));

        assertEquals(2, rects.size());
        assertEquals(Set.of(new ClaimRect("world", 0, 0, 0, 0),
                new ClaimRect("world", 2, 0, 2, 0)), new HashSet<>(rects));
    }

    @Test
    void lShapeIsCoveredWithoutOverlap() {
        List<ClaimKey> claims = List.of(
                new ClaimKey("world", 0, 0),
                new ClaimKey("world", 1, 0),
                new ClaimKey("world", 0, 1));
        List<ClaimRect> rects = ClaimAreaMerger.merge(claims);

        assertEquals(claims.size(), coveredChunks(rects).size());
        assertTrue(coveredChunks(rects).containsAll(claims));
    }

    @Test
    void worldsAreKeptApart() {
        List<ClaimRect> rects = ClaimAreaMerger.merge(List.of(
                new ClaimKey("world", 0, 0),
                new ClaimKey("world_nether", 0, 0)));

        assertEquals(2, rects.size());
        assertTrue(rects.stream().anyMatch(rect -> rect.world().equals("world")));
        assertTrue(rects.stream().anyMatch(rect -> rect.world().equals("world_nether")));
    }

    @Test
    void emptyInputHasNoRectangles() {
        assertTrue(ClaimAreaMerger.merge(List.of()).isEmpty());
    }

    private static Set<ClaimKey> coveredChunks(List<ClaimRect> rects) {
        Set<ClaimKey> covered = new HashSet<>();
        for (ClaimRect rect : rects) {
            for (int x = rect.minX(); x <= rect.maxX(); x++) {
                for (int z = rect.minZ(); z <= rect.maxZ(); z++) {
                    covered.add(new ClaimKey(rect.world(), x, z));
                }
            }
        }
        return covered;
    }
}
