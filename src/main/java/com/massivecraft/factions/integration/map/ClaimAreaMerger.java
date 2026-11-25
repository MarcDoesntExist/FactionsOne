package com.massivecraft.factions.integration.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Merges adjacent claimed chunks into rectangles so the web map receives far
 * fewer markers than one per chunk.
 *
 * @author MarcDoesntExists
 */
public final class ClaimAreaMerger {
    private ClaimAreaMerger() {
    }

    /**
     * @param claims claimed chunks, possibly from several worlds
     * @return non-overlapping rectangles covering exactly the input chunks
     */
    public static List<ClaimRect> merge(Collection<ClaimKey> claims) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }

        Map<String, TreeMap<Integer, TreeSet<Integer>>> byWorld = new TreeMap<>();
        for (ClaimKey claim : claims) {
            byWorld.computeIfAbsent(claim.world(), ignored -> new TreeMap<>())
                    .computeIfAbsent(claim.z(), ignored -> new TreeSet<>())
                    .add(claim.x());
        }

        List<ClaimRect> result = new ArrayList<>();
        for (Map.Entry<String, TreeMap<Integer, TreeSet<Integer>>> world : byWorld.entrySet()) {
            mergeWorld(world.getKey(), world.getValue(), result);
        }
        result.sort(Comparator.comparing(ClaimRect::world)
                .thenComparingInt(ClaimRect::minZ)
                .thenComparingInt(ClaimRect::minX));
        return result;
    }

    private static void mergeWorld(String world, TreeMap<Integer, TreeSet<Integer>> rows,
                                   List<ClaimRect> result) {
        Map<Range, int[]> open = new HashMap<>();
        for (Map.Entry<Integer, TreeSet<Integer>> row : rows.entrySet()) {
            int z = row.getKey();
            List<Range> runs = runs(row.getValue());

            Iterator<Map.Entry<Range, int[]>> iterator = open.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Range, int[]> entry = iterator.next();
                int[] span = entry.getValue();
                if (span[1] != z - 1 || !runs.contains(entry.getKey())) {
                    result.add(new ClaimRect(world, entry.getKey().minX(), span[0],
                            entry.getKey().maxX(), span[1]));
                    iterator.remove();
                }
            }

            for (Range run : runs) {
                int[] span = open.get(run);
                if (span == null) {
                    open.put(run, new int[]{z, z});
                } else {
                    span[1] = z;
                }
            }
        }

        for (Map.Entry<Range, int[]> entry : open.entrySet()) {
            int[] span = entry.getValue();
            result.add(new ClaimRect(world, entry.getKey().minX(), span[0],
                    entry.getKey().maxX(), span[1]));
        }
    }

    private static List<Range> runs(TreeSet<Integer> xs) {
        List<Range> runs = new ArrayList<>();
        int start = Integer.MIN_VALUE;
        int previous = Integer.MIN_VALUE;
        for (int x : xs) {
            if (start == Integer.MIN_VALUE) {
                start = x;
            } else if (x != previous + 1) {
                runs.add(new Range(start, previous));
                start = x;
            }
            previous = x;
        }
        if (start != Integer.MIN_VALUE) {
            runs.add(new Range(start, previous));
        }
        return runs;
    }

    private record Range(int minX, int maxX) {
    }
}
