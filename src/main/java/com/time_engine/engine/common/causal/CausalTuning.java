package com.time_engine.engine.common.causal;

public record CausalTuning(
        CausalTrackingPolicy trackingPolicy, CausalTargetSelectionRules selectionRules) {
    public CausalTuning {
        if (trackingPolicy == null) {
            throw new IllegalArgumentException("trackingPolicy must not be null");
        }
        if (selectionRules == null) {
            throw new IllegalArgumentException("selectionRules must not be null");
        }
    }

    public static CausalTuning of(
            double sessionRadius,
            double causalLockedTrackingRadius,
            double maxCausalTrackingDistance) {
        return new CausalTuning(
                new CausalTrackingPolicy(
                        sessionRadius, causalLockedTrackingRadius, maxCausalTrackingDistance),
                CausalTargetSelectionRules.DEFAULT);
    }
}
