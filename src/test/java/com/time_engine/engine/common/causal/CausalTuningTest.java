package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class CausalTuningTest {
    @Test
    void factoryCreatesTrackingPolicyWithDefaultSelectionRules() {
        CausalTuning tuning = CausalTuning.of(5.0D, 12.0D, 16.0D);

        assertEquals(5.0D, tuning.trackingPolicy().sessionRadius());
        assertEquals(12.0D, tuning.trackingPolicy().causalLockedTrackingRadius());
        assertEquals(16.0D, tuning.trackingPolicy().maxCausalTrackingDistance());
        assertSame(CausalTargetSelectionRules.DEFAULT, tuning.selectionRules());
    }
}
