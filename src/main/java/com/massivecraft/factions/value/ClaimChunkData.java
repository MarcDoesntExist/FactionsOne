package com.massivecraft.factions.value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Accumulates the valued contents of one claimed chunk without retaining any
 * Bukkit objects. The maps are copied by value into the calculator.
 *
 * @author MarcDoesntExists
 */
public final class ClaimChunkData {

    private final Map<String, Integer> spawners = new HashMap<String, Integer>();
    private final Map<String, Integer> containers = new HashMap<String, Integer>();
    private final Map<String, Integer> items = new HashMap<String, Integer>();

    public void addSpawner(String entityType) {
        if (entityType != null) {
            this.spawners.merge(entityType, 1, Integer::sum);
        }
    }

    public void addContainer(String material) {
        if (material != null) {
            this.containers.merge(material, 1, Integer::sum);
        }
    }

    public void addItem(String material, int amount) {
        if (material != null && amount > 0) {
            this.items.merge(material, amount, Integer::sum);
        }
    }

    public Map<String, Integer> spawners() {
        return Collections.unmodifiableMap(this.spawners);
    }

    public Map<String, Integer> containers() {
        return Collections.unmodifiableMap(this.containers);
    }

    public Map<String, Integer> items() {
        return Collections.unmodifiableMap(this.items);
    }
}
