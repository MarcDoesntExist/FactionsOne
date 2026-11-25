package com.massivecraft.factions;

import java.util.concurrent.TimeUnit;

/**
 * Pure boundary arithmetic for daily and weekly mission resets.
 *
 * @author MarcDoesntExists
 */
public final class MissionResetSchedule {

    private MissionResetSchedule() {
    }

    public static long intervalMillis(FeatureConfig.MissionPeriod period) {
        return switch (period) {
            case DAILY -> TimeUnit.DAYS.toMillis(1);
            case WEEKLY -> TimeUnit.DAYS.toMillis(7);
            case NONE -> 0L;
        };
    }

    /**
     * Latest boundary at or before {@code now}; returns {@code lastResetAt}
     * when the period is not due yet.
     */
    public static long latestBoundary(long lastResetAt, FeatureConfig.MissionPeriod period,
                                      long now) {
        long interval = intervalMillis(period);
        if (lastResetAt <= 0 || interval <= 0 || now < lastResetAt + interval) {
            return lastResetAt;
        }
        return lastResetAt + ((now - lastResetAt) / interval) * interval;
    }

    public static long nextBoundary(long lastResetAt, FeatureConfig.MissionPeriod period,
                                    long now) {
        if (lastResetAt <= 0 || period == FeatureConfig.MissionPeriod.NONE) {
            return 0L;
        }
        return latestBoundary(lastResetAt, period, now) + intervalMillis(period);
    }
}
