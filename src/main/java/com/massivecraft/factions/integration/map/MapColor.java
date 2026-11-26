package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.struct.Rel;

import java.util.Locale;

/**
 * Pure color policy for the web map.
 *
 * <p>Without a reference faction every faction gets a stable color derived
 * from its tag. With a reference faction configured, the claim is colored by
 * the owner's relation to that faction. SafeZone and WarZone keep fixed
 * colors so they remain recognizable.</p>
 *
 * @author MarcDoesntExists
 */
public final class MapColor {
    private static final int[] FACTION_PALETTE = {
        0x4CAF50, 0x2196F3, 0x9C27B0, 0xFF9800, 0x00BCD4, 0x795548,
        0x3F51B5, 0xE91E63, 0x009688, 0x8BC34A, 0xFF5722, 0x607D8B
    };

    private MapColor() {
    }

    public static int colorFor(String factionId, String tag, Rel relation, boolean relationMode) {
        if ("-1".equals(factionId)) {
            return 0xF2C94C;
        }
        if ("-2".equals(factionId)) {
            return 0xEB5757;
        }
        if (relationMode && relation != null) {
            return relationColor(relation);
        }
        return factionColor(tag);
    }

    public static int relationColor(Rel relation) {
        if (relation == null) {
            return 0xFFFFFF;
        }
        return switch (relation) {
            case LEADER, COLEADER, OFFICER, MEMBER, RECRUIT -> 0x55FF55;
            case ALLY -> 0x9C27B0;
            case TRUCE -> 0xFF55FF;
            case NEUTRAL -> 0xFFFFFF;
            case ENEMY -> 0xFF5555;
        };
    }

    public static int factionColor(String tag) {
        String key = tag == null ? "" : tag.toUpperCase(Locale.ROOT);
        return FACTION_PALETTE[Math.floorMod(key.hashCode(), FACTION_PALETTE.length)];
    }
}
