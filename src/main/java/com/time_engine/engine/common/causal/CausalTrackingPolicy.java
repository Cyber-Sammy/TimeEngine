package com.time_engine.engine.common.causal;

import net.minecraft.world.phys.Vec3;

public record CausalTrackingPolicy(
        double sessionRadius, double causalLockedTrackingRadius, double maxCausalTrackingDistance) {
    public CausalTrackingPolicy {
        validatePositive("sessionRadius", sessionRadius);
        validatePositive("causalLockedTrackingRadius", causalLockedTrackingRadius);
        validatePositive("maxCausalTrackingDistance", maxCausalTrackingDistance);
        if (causalLockedTrackingRadius < sessionRadius) {
            throw new IllegalArgumentException(
                    "causalLockedTrackingRadius must be at least sessionRadius");
        }
        if (maxCausalTrackingDistance < causalLockedTrackingRadius) {
            throw new IllegalArgumentException(
                    "maxCausalTrackingDistance must be at least causalLockedTrackingRadius");
        }
    }

    public boolean allowsUnlockedCandidate(Vec3 userPosition, CausalTargetCandidate candidate) {
        return distance(userPosition, candidate) <= sessionRadius;
    }

    public boolean keepsLockedTarget(Vec3 userPosition, CausalTargetCandidate candidate) {
        return distance(userPosition, candidate) <= maxCausalTrackingDistance;
    }

    public boolean isInsidePreferredLockedTrackingRadius(
            Vec3 userPosition, CausalTargetCandidate candidate) {
        return distance(userPosition, candidate) <= causalLockedTrackingRadius;
    }

    public boolean expiresLockedTarget(Vec3 userPosition, CausalTargetCandidate candidate) {
        return distance(userPosition, candidate) > maxCausalTrackingDistance;
    }

    private static double distance(Vec3 userPosition, CausalTargetCandidate candidate) {
        return candidate.distanceTo(userPosition);
    }

    private static void validatePositive(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
