package com.time_engine.engine.common.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TemporalAttackValidatorTest {
    @Test
    void acceptsClientTickWithinAllowedDrift() {
        assertTrue(TemporalAttackValidator.isPerceivedTickWithinDrift(100.0D, 102.5D, 3.0D));
    }

    @Test
    void acceptsExactDriftBoundary() {
        assertTrue(TemporalAttackValidator.isPerceivedTickWithinDrift(100.0D, 103.0D, 3.0D));
    }

    @Test
    void rejectsClientTickOutsideAllowedDrift() {
        assertFalse(TemporalAttackValidator.isPerceivedTickWithinDrift(100.0D, 103.01D, 3.0D));
    }

    @Test
    void rejectsNonFiniteTicks() {
        assertFalse(TemporalAttackValidator.isPerceivedTickWithinDrift(Double.NaN, 100.0D, 3.0D));
        assertFalse(
                TemporalAttackValidator.isPerceivedTickWithinDrift(
                        100.0D, Double.POSITIVE_INFINITY, 3.0D));
    }
}
