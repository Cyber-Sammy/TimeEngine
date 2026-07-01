package com.time_engine.common.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.time_engine.common.policy.TemporalPolicy.Decision;
import org.junit.jupiter.api.Test;

class TemporalPolicyDefaultsTest {
    @Test
    void snapshotAllowsPlayersAndMobsByDefault() {
        assertEquals(Decision.ALLOW, TemporalPolicyDefaults.snapshot(true, false, false));
        assertEquals(Decision.ALLOW, TemporalPolicyDefaults.snapshot(false, true, false));
    }

    @Test
    void snapshotIgnoresProjectilesByDefault() {
        assertEquals(Decision.IGNORE, TemporalPolicyDefaults.snapshot(false, false, true));
    }

    @Test
    void phantomCombatAllowsPlayersAndMobsByDefault() {
        assertEquals(Decision.ALLOW, TemporalPolicyDefaults.phantomCombat(true, false));
        assertEquals(Decision.ALLOW, TemporalPolicyDefaults.phantomCombat(false, true));
    }

    @Test
    void phantomCombatIgnoresProjectilesAndUnknownEntitiesByDefault() {
        assertEquals(Decision.IGNORE, TemporalPolicyDefaults.phantomCombat(false, false));
    }
}
