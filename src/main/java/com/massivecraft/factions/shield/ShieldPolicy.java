package com.massivecraft.factions.shield;

/**
 * Pure time-window rules for faction grace and shields.
 *
 * @author MarcDoesntExists
 */
public final class ShieldPolicy {

    private static final long HOUR_MILLIS = 3_600_000L;
    private static final long MAX_HOURS = 24L * 365L;

    private ShieldPolicy() {
    }

    public static boolean active(long until, long now) {
        return until > now;
    }

    public static boolean blocks(long graceUntil, long shieldUntil, long now) {
        return active(graceUntil, now) || active(shieldUntil, now);
    }

    /**
     * Adds a number of hours to a timestamp, clamping negative values to zero
     * and the duration to one year.
     */
    public static long addHours(long now, int hours) {
        long clamped = Math.max(0, Math.min(hours, MAX_HOURS));
        return now + clamped * HOUR_MILLIS;
    }

    /**
     * Remaining whole minutes, rounded up; zero when the window is over.
     */
    public static long remainingMinutes(long until, long now) {
        if (!active(until, now)) {
            return 0;
        }
        return Math.max(1, (until - now + 59_999L) / 60_000L);
    }
}
