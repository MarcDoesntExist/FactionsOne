package com.massivecraft.factions.rank;

import java.util.Comparator;
import java.util.Locale;

/**
 * One immutable row of the faction leaderboard.
 *
 * @author MarcDoesntExists
 */
public record FactionRankEntry(String id, String tag, double power, int land, int members,
                               double bank, int points) {

    /** Sortable metrics exposed by {@code /f top}. */
    public enum Metric {
        POWER("power"),
        LAND("land"),
        MEMBERS("members"),
        BANK("bank"),
        POINTS("points");

        private final String key;

        Metric(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }

        public double value(FactionRankEntry entry) {
            return switch (this) {
                case POWER -> entry.power();
                case LAND -> entry.land();
                case MEMBERS -> entry.members();
                case BANK -> entry.bank();
                case POINTS -> entry.points();
            };
        }

        public static Metric parse(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Metric metric : values()) {
                if (metric.key.equals(normalized)) {
                    return metric;
                }
            }
            return null;
        }
    }

    /** Descending by the chosen metric, ties broken by tag. */
    public static Comparator<FactionRankEntry> order(Metric metric) {
        return Comparator.comparingDouble(metric::value).reversed()
                .thenComparing(FactionRankEntry::tag, String.CASE_INSENSITIVE_ORDER);
    }
}
