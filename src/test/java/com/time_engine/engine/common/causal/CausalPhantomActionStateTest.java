package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CausalPhantomActionStateTest {
    @Test
    void attackSwingRequiresUnblockedSwing() {
        CausalPhantomActionState state =
                CausalPhantomActionState.fromLivingFlags(true, false, false);

        assertTrue(state.swinging());
        assertTrue(state.attacking());
        assertFalse(state.blocking());
        assertFalse(state.usingItem());
    }

    @Test
    void blockingSwingIsNotAttack() {
        CausalPhantomActionState state = CausalPhantomActionState.fromLivingFlags(true, true, true);

        assertTrue(state.swinging());
        assertFalse(state.attacking());
        assertTrue(state.blocking());
        assertTrue(state.usingItem());
    }
}
