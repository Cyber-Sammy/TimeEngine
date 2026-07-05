package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalTrackingPolicyTest {
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final Vec3 USER_POSITION = Vec3.ZERO;
    private static final CausalTrackingPolicy POLICY = new CausalTrackingPolicy(5.0D, 12.0D, 16.0D);

    @Test
    void unlockedCandidateUsesSessionRadius() {
        assertTrue(POLICY.allowsUnlockedCandidate(USER_POSITION, candidate(5.0D)));
        assertFalse(POLICY.allowsUnlockedCandidate(USER_POSITION, candidate(5.1D)));
    }

    @Test
    void lockedTargetIsKeptUntilMaxCausalTrackingDistance() {
        assertTrue(POLICY.keepsLockedTarget(USER_POSITION, candidate(11.9D)));
        assertTrue(POLICY.keepsLockedTarget(USER_POSITION, candidate(14.0D)));
        assertTrue(POLICY.keepsLockedTarget(USER_POSITION, candidate(16.0D)));
        assertFalse(POLICY.keepsLockedTarget(USER_POSITION, candidate(16.1D)));
    }

    @Test
    void preferredLockedTrackingRadiusIsDistinctFromHardKeepCap() {
        assertTrue(POLICY.isInsidePreferredLockedTrackingRadius(USER_POSITION, candidate(11.9D)));
        assertFalse(POLICY.isInsidePreferredLockedTrackingRadius(USER_POSITION, candidate(12.1D)));
    }

    @Test
    void lockedTargetExpiresBeyondMaxCausalTrackingDistance() {
        assertFalse(POLICY.expiresLockedTarget(USER_POSITION, candidate(16.0D)));
        assertTrue(POLICY.expiresLockedTarget(USER_POSITION, candidate(16.1D)));
    }

    private static CausalTargetCandidate candidate(double distance) {
        Vec3 position = new Vec3(distance, 0.0D, 0.0D);
        return CausalTargetCandidate.target(
                TARGET_ID,
                position,
                new AABB(distance - 0.5D, 0.0D, -0.5D, distance + 0.5D, 1.8D, 0.5D));
    }
}
