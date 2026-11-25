package com.massivecraft.factions;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Headless checks for mission reset scheduling.
 *
 * @author MarcDoesntExists
 */
class MissionResetScheduleTest {

    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    @Test
    void dailyBoundaryAdvancesOnlyWhenDue() {
        long last = 10 * DAY;
        assertEquals(last, MissionResetSchedule.latestBoundary(last,
                FeatureConfig.MissionPeriod.DAILY, last + DAY - 1));
        assertEquals(last + DAY, MissionResetSchedule.latestBoundary(last,
                FeatureConfig.MissionPeriod.DAILY, last + DAY));
        assertEquals(last + 3 * DAY, MissionResetSchedule.latestBoundary(last,
                FeatureConfig.MissionPeriod.DAILY, last + 3 * DAY + 5000));
    }

    @Test
    void weeklyAndNoneRespectTheirIntervals() {
        long last = 10 * DAY;
        assertEquals(last, MissionResetSchedule.latestBoundary(last,
                FeatureConfig.MissionPeriod.WEEKLY, last + 6 * DAY));
        assertEquals(last + 7 * DAY, MissionResetSchedule.latestBoundary(last,
                FeatureConfig.MissionPeriod.WEEKLY, last + 8 * DAY));
        assertEquals(0L, MissionResetSchedule.nextBoundary(last,
                FeatureConfig.MissionPeriod.NONE, last + 8 * DAY));
        assertEquals(0L, MissionResetSchedule.nextBoundary(0L,
                FeatureConfig.MissionPeriod.DAILY, last + 8 * DAY));
    }

    @Test
    void nextBoundaryIsAlwaysInTheFuture() {
        long last = 10 * DAY;
        long now = last + 2 * DAY + 1234;
        long next = MissionResetSchedule.nextBoundary(last,
                FeatureConfig.MissionPeriod.DAILY, now);
        assertEquals(last + 3 * DAY, next);
    }
}
