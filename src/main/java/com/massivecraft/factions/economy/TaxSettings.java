package com.massivecraft.factions.economy;

/**
 * Immutable snapshot of the faction-tax rates read from the configuration
 * before a tax run. Negative or non-finite values are treated as zero.
 *
 * @author MarcDoesntExists
 */
public record TaxSettings(boolean enabled, double base, double perMember, double perChunk,
                          double cap, double percent, Mode mode) {

    public enum Mode {
        FLAT,
        PERCENT;

        public static Mode parse(String value) {
            if (value == null) {
                return FLAT;
            }
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return FLAT;
            }
        }
    }

    /**
     * Backwards-compatible flat constructor used by existing callers and tests.
     */
    public TaxSettings(boolean enabled, double base, double perMember, double perChunk,
                       double cap) {
        this(enabled, base, perMember, perChunk, cap, 0.0, Mode.FLAT);
    }

    public TaxSettings {
        base = sanitize(base);
        perMember = sanitize(perMember);
        perChunk = sanitize(perChunk);
        cap = sanitize(cap);
        percent = sanitize(percent);
        mode = mode == null ? Mode.FLAT : mode;
    }

    public static TaxSettings disabled() {
        return new TaxSettings(false, 0.0, 0.0, 0.0, 0.0);
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) && value > 0.0 ? value : 0.0;
    }
}
