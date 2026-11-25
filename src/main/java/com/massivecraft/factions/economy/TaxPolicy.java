package com.massivecraft.factions.economy;

/**
 * Pure arrears/insolvency policy for the faction tax.
 *
 * @author MarcDoesntExists
 */
public final class TaxPolicy {

    private TaxPolicy() {
    }

    /**
     * Result of one tax period for a faction.
     *
     * @param arrears   consecutive missed periods after this one
     * @param insolvent true when the grace allowance is exhausted
     */
    public record Outcome(int arrears, boolean insolvent) {
    }

    /**
     * Applies one period. A successful payment clears the arrears; a failed
     * one increases them and turns the faction insolvent only after more than
     * {@code gracePeriods} consecutive failures.
     */
    public static Outcome evaluate(boolean paid, int arrears, int gracePeriods) {
        if (paid) {
            return new Outcome(0, false);
        }
        int next = Math.max(0, arrears) + 1;
        return new Outcome(next, next > Math.max(0, gracePeriods));
    }
}
