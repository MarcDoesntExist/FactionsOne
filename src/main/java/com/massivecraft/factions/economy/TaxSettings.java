package com.massivecraft.factions.economy;

/**
 * Immutable snapshot of the faction-tax rates read from the configuration
 * before a tax run. Negative or non-finite values are treated as zero.
 *
 * @author MarcDoesntExists
 */
public record TaxSettings(boolean enabled, double base, double perMember, double perChunk, double cap) {

    public TaxSettings {
        base = sanitize(base);
        perMember = sanitize(perMember);
        perChunk = sanitize(perChunk);
        cap = sanitize(cap);
    }

    public static TaxSettings disabled() {
        return new TaxSettings(false, 0.0, 0.0, 0.0, 0.0);
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) && value > 0.0 ? value : 0.0;
    }
}
