package com.time_engine.engine.common.causal;

public record CausalTargetSelectionRules(
        int minimumLockTicks,
        int hardLockTicks,
        double targetSwitchScoreThreshold,
        double intentConeDot,
        double distanceWeight,
        double lookWeight,
        double movementWeight,
        double previousTargetBonus,
        double hardLockBonus) {
    public static final CausalTargetSelectionRules DEFAULT =
            new CausalTargetSelectionRules(5, 20, 1.5D, 0.65D, 10.0D, 6.0D, 3.0D, 2.0D, 8.0D);

    public CausalTargetSelectionRules {
        if (minimumLockTicks < 0) {
            throw new IllegalArgumentException("minimumLockTicks must not be negative");
        }
        if (hardLockTicks < 0) {
            throw new IllegalArgumentException("hardLockTicks must not be negative");
        }
        validateFinite("targetSwitchScoreThreshold", targetSwitchScoreThreshold);
        validateFinite("intentConeDot", intentConeDot);
        validateFinite("distanceWeight", distanceWeight);
        validateFinite("lookWeight", lookWeight);
        validateFinite("movementWeight", movementWeight);
        validateFinite("previousTargetBonus", previousTargetBonus);
        validateFinite("hardLockBonus", hardLockBonus);
    }

    private static void validateFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
