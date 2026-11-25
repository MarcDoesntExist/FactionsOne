package com.massivecraft.factions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for chunk/region conversions and FLocation value semantics.
 *
 * @author MarcDoesntExists
 */
class FLocationTest {

    @Test
    void blockCoordinatesConvertToChunksAndBack() {
        assertEquals(0, FLocation.blockToChunk(0));
        assertEquals(0, FLocation.blockToChunk(15));
        assertEquals(1, FLocation.blockToChunk(16));
        assertEquals(1, FLocation.blockToChunk(31));
        assertEquals(-1, FLocation.blockToChunk(-1));
        assertEquals(-1, FLocation.blockToChunk(-16));
        assertEquals(-2, FLocation.blockToChunk(-17));

        assertEquals(16, FLocation.chunkToBlock(1));
        assertEquals(-16, FLocation.chunkToBlock(-1));
    }

    @Test
    void blockCoordinatesConvertToRegionsAndBack() {
        assertEquals(0, FLocation.blockToRegion(0));
        assertEquals(0, FLocation.blockToRegion(511));
        assertEquals(1, FLocation.blockToRegion(512));
        assertEquals(-1, FLocation.blockToRegion(-1));
        assertEquals(-1, FLocation.blockToRegion(-512));
        assertEquals(-2, FLocation.blockToRegion(-513));

        assertEquals(512, FLocation.regionToBlock(1));
        assertEquals(-512, FLocation.regionToBlock(-1));
        assertEquals(32, FLocation.regionToChunk(1));
        assertEquals(-32, FLocation.regionToChunk(-1));
    }

    @Test
    void chunkCoordinatesConvertToRegionsAndBack() {
        assertEquals(0, FLocation.chunkToRegion(0));
        assertEquals(0, FLocation.chunkToRegion(31));
        assertEquals(1, FLocation.chunkToRegion(32));
        assertEquals(-1, FLocation.chunkToRegion(-1));
        assertEquals(-1, FLocation.chunkToRegion(-32));
        assertEquals(-2, FLocation.chunkToRegion(-33));
    }

    @Test
    void coordinateStringAndRelativeLocationsAreStable() {
        FLocation location = new FLocation("world", 3, -7);

        assertEquals("3,-7", location.getCoordString());
        assertEquals("[world,3,-7]", location.toString());

        FLocation relative = location.getRelative(2, 5);
        assertEquals("world", relative.getWorldName());
        assertEquals(5L, relative.getX());
        assertEquals(-2L, relative.getZ());
    }

    @Test
    void distanceUsesChunkCoordinates() {
        FLocation origin = new FLocation("world", 0, 0);
        FLocation target = new FLocation("world", 3, 4);

        assertEquals(5.0, origin.getDistanceTo(target));
        assertEquals(5.0, target.getDistanceTo(origin));
    }

    @Test
    void circleIsEmptyForNonPositiveRadius() {
        FLocation center = new FLocation("world", 10, 10);

        assertTrue(center.getCircle(0).isEmpty());
        assertTrue(center.getCircle(-1).isEmpty());
    }

    @Test
    void circleIncludesOnlyCellsInsideRadius() {
        FLocation center = new FLocation("world", 10, 10);

        Set<FLocation> radiusOne = center.getCircle(1);
        assertEquals(5, radiusOne.size());
        assertTrue(radiusOne.contains(center));
        assertTrue(radiusOne.contains(new FLocation("world", 11, 10)));
        assertFalse(radiusOne.contains(new FLocation("world", 11, 11)));

        assertEquals(13, center.getCircle(2).size());
    }

    @Test
    void areaIsInclusiveAndAcceptsReversedCorners() {
        Set<FLocation> area = FLocation.getArea(
                new FLocation("world", 0, 0),
                new FLocation("world", 1, 1));
        assertEquals(4, area.size());
        assertTrue(area.contains(new FLocation("world", 0, 0)));
        assertTrue(area.contains(new FLocation("world", 1, 1)));

        Set<FLocation> reversed = FLocation.getArea(
                new FLocation("world", 2, 3),
                new FLocation("world", 0, 0));
        assertEquals(12, reversed.size());
        assertTrue(reversed.contains(new FLocation("world", 0, 0)));
        assertTrue(reversed.contains(new FLocation("world", 2, 3)));
    }

    @Test
    void equalityUsesWorldAndChunkCoordinates() {
        FLocation first = new FLocation("world", 4, 9);
        FLocation same = new FLocation("world", 4, 9);
        FLocation otherChunk = new FLocation("world", 4, 10);
        FLocation otherWorld = new FLocation("nether", 4, 9);

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, otherChunk);
        assertNotEquals(first, otherWorld);
        assertNotEquals(first, null);
        assertNotEquals(first, "world,4,9");
    }

    @Test
    void nullWorldIsSupportedAsAValue() {
        FLocation first = new FLocation(null, 0, 0);
        FLocation same = new FLocation(null, 0, 0);
        FLocation other = new FLocation("world", 0, 0);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, other);
    }

    @Test
    void settersUpdateTheStoredCoordinates() {
        FLocation location = new FLocation();
        location.setWorldName("world_nether");
        location.setX(12);
        location.setZ(-34);

        assertEquals("world_nether", location.getWorldName());
        assertEquals(12L, location.getX());
        assertEquals(-34L, location.getZ());
        assertEquals("12,-34", location.getCoordString());
    }
}
