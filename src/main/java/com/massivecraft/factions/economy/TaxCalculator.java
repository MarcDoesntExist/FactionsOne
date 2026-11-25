package com.massivecraft.factions.economy;

/**
 * Pure faction-tax amount calculation. It has no Bukkit dependency so the
 * rounding and cap rules can be tested without a running server.
 *
 * @author MarcDoesntExists
 */
public final class TaxCalculator {

    private TaxCalculator() {
    }

    /**
     * Computes the tax owed for one period, rounded to cents. A cap of zero or
     * less means "no cap".
     *
     * @param members  current faction members, negative values count as zero
     * @param land     rounded claimed chunks, negative values count as zero
     * @param settings rates to apply
     * @return the amount in the server currency, never negative
     */
    public static double due(int members, int land, TaxSettings settings) {
        return due(members, land, 0.0, settings);
    }

    /**
     * Computes the tax for one period. {@code PERCENT} mode uses the faction
     * bank balance; {@code FLAT} mode keeps the historical per-member/per-chunk
     * formula.
     */
    public static double due(int members, int land, double balance, TaxSettings settings) {
        if (settings == null || !settings.enabled()) {
            return 0.0;
        }

        double raw;
        if (settings.mode() == TaxSettings.Mode.PERCENT) {
            raw = Math.max(0.0, balance) * settings.percent() / 100.0;
        } else {
            raw = settings.base()
                    + settings.perMember() * Math.max(0, members)
                    + settings.perChunk() * Math.max(0, land);
        }
        if (!Double.isFinite(raw) || raw <= 0.0) {
            return 0.0;
        }

        long cents = Math.round(raw * 100.0);
        if (settings.cap() > 0.0) {
            cents = Math.min(cents, Math.round(settings.cap() * 100.0));
        }
        return cents / 100.0;
    }
}
