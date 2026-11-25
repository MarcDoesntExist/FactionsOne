package com.massivecraft.factions.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure tax arrears and insolvency policy.
 *
 * @author MarcDoesntExists
 */
class TaxPolicyTest {

    @Test
    void paymentClearsArrears() {
        TaxPolicy.Outcome outcome = TaxPolicy.evaluate(true, 4, 3);
        assertEquals(0, outcome.arrears());
        assertFalse(outcome.insolvent());
    }

    @Test
    void firstFailureStaysInsideGrace() {
        TaxPolicy.Outcome outcome = TaxPolicy.evaluate(false, 0, 1);
        assertEquals(1, outcome.arrears());
        assertFalse(outcome.insolvent());
    }

    @Test
    void failureBeyondGraceIsInsolvent() {
        TaxPolicy.Outcome outcome = TaxPolicy.evaluate(false, 1, 1);
        assertEquals(2, outcome.arrears());
        assertTrue(outcome.insolvent());
    }

    @Test
    void zeroGraceFailsOnFirstMiss() {
        TaxPolicy.Outcome outcome = TaxPolicy.evaluate(false, 0, 0);
        assertEquals(1, outcome.arrears());
        assertTrue(outcome.insolvent());
    }

    @Test
    void negativeArrearsAreNormalized() {
        TaxPolicy.Outcome outcome = TaxPolicy.evaluate(false, -7, 3);
        assertEquals(1, outcome.arrears());
        assertFalse(outcome.insolvent());
    }
}
