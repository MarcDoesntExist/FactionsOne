package com.massivecraft.factions.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the pure faction-tax amount calculation.
 *
 * @author MarcDoesntExists
 */
class TaxCalculatorTest {

    private static TaxSettings settings(double base, double perMember, double perChunk, double cap) {
        return new TaxSettings(true, base, perMember, perChunk, cap);
    }

    @Test
    void disabledTaxCostsNothing() {
        assertEquals(0.0, TaxCalculator.due(5, 10, TaxSettings.disabled()));
        assertEquals(0.0, TaxCalculator.due(5, 10, null));
    }

    @Test
    void baseOnly() {
        assertEquals(50.0, TaxCalculator.due(0, 0, settings(50.0, 0.0, 0.0, 0.0)));
    }

    @Test
    void addsPerMemberAndPerChunk() {
        assertEquals(24.5, TaxCalculator.due(4, 6, settings(10.0, 2.5, 0.75, 0.0)));
    }

    @Test
    void appliesCap() {
        assertEquals(40.0, TaxCalculator.due(100, 100, settings(10.0, 2.0, 1.0, 40.0)));
    }

    @Test
    void zeroCapMeansNoCap() {
        assertEquals(210.0, TaxCalculator.due(100, 100, settings(10.0, 1.0, 1.0, 0.0)));
    }

    @Test
    void roundsToCents() {
        assertEquals(1.0, TaxCalculator.due(0, 3, settings(0.0, 0.0, 0.333, 0.0)));
        assertEquals(0.67, TaxCalculator.due(0, 2, settings(0.0, 0.0, 0.333, 0.0)));
    }

    @Test
    void percentModeUsesBankBalanceAndCap() {
        TaxSettings percent = new TaxSettings(true, 0.0, 0.0, 0.0, 50.0, 10.0,
                TaxSettings.Mode.PERCENT);
        assertEquals(10.0, TaxCalculator.due(0, 0, 100.0, percent));
        assertEquals(50.0, TaxCalculator.due(0, 0, 1000.0, percent));
        assertEquals(0.0, TaxCalculator.due(0, 0, -5.0, percent));
    }

    @Test
    void percentZeroBalanceAndRounding() {
        TaxSettings percent = new TaxSettings(true, 0.0, 0.0, 0.0, 0.0, 10.0,
                TaxSettings.Mode.PERCENT);
        assertEquals(0.0, TaxCalculator.due(0, 0, 0.0, percent));
        assertEquals(10.01, TaxCalculator.due(0, 0, 100.05, percent));
        assertEquals(0.0, TaxCalculator.due(0, 0, 100.0,
                new TaxSettings(true, 0.0, 0.0, 0.0, 0.0, -5.0,
                        TaxSettings.Mode.PERCENT)));
    }

    @Test
    void invalidModeFallsBackToFlat() {
        assertEquals(TaxSettings.Mode.FLAT, TaxSettings.Mode.parse("unknown"));
        assertEquals(TaxSettings.Mode.PERCENT, TaxSettings.Mode.parse("percent"));
    }

    @Test
    void clampsNegativeInputsAndRates() {
        assertEquals(5.0, TaxCalculator.due(-3, -4, settings(5.0, -2.0, -1.0, 0.0)));
        assertEquals(0.0, TaxCalculator.due(1, 1, new TaxSettings(true, Double.NaN, 0.0, 0.0, 0.0)));
    }
}
