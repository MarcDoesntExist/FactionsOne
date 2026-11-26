package com.massivecraft.factions.value;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Headless coverage for the claim value arithmetic.
 *
 * @author MarcDoesntExists
 */
class ClaimValueCalculatorTest {

    private static final Map<String, Double> SPAWNERS = Map.of("ZOMBIE", 100.0);
    private static final Map<String, Double> CONTAINERS = Map.of("CHEST", 5.0);
    private static final Map<String, Double> ITEMS = Map.of("DIAMOND", 5.0, "EMERALD", 6.0);

    @Test
    void sumsSpawnersContainersAndItems() {
        ClaimChunkData data = new ClaimChunkData();
        data.addSpawner("ZOMBIE");
        data.addContainer("CHEST");
        data.addItem("DIAMOND", 3);
        data.addItem("EMERALD", 2);

        assertEquals(132.0, ClaimValueCalculator.valueOf(data, SPAWNERS, CONTAINERS, ITEMS));
    }

    @Test
    void unknownKeysAreWorthNothing() {
        ClaimChunkData data = new ClaimChunkData();
        data.addSpawner("CREEPER");
        data.addContainer("BARREL");
        data.addItem("DIRT", 64);

        assertEquals(0.0, ClaimValueCalculator.valueOf(data, SPAWNERS, CONTAINERS, ITEMS));
    }

    @Test
    void negativeAndNonFinitePricesAreIgnored() {
        ClaimChunkData data = new ClaimChunkData();
        data.addSpawner("ZOMBIE");
        data.addContainer("CHEST");
        data.addItem("DIAMOND", 2);

        Map<String, Double> badSpawners = Map.of("ZOMBIE", -100.0);
        Map<String, Double> badContainers = Map.of("CHEST", Double.NaN);
        Map<String, Double> badItems = Map.of("DIAMOND", Double.POSITIVE_INFINITY);

        assertEquals(0.0,
                ClaimValueCalculator.valueOf(data, badSpawners, badContainers, badItems));
    }

    @Test
    void emptyOrNullInputIsZero() {
        assertEquals(0.0, ClaimValueCalculator.valueOf(
                new ClaimChunkData(), SPAWNERS, CONTAINERS, ITEMS));
        assertEquals(0.0, ClaimValueCalculator.valueOf(
                null, SPAWNERS, CONTAINERS, ITEMS));
        assertEquals(0.0, ClaimValueCalculator.valueOf(
                new ClaimChunkData(), null, null, null));
    }
}
