package com.time_engine.engine.common.intercept;

import com.time_engine.engine.common.temporal.TemporalLayerRelation;

final class TemporalInterceptRules {
    private TemporalInterceptRules() {}

    static boolean allowsIntercept(double observerScale, double targetScale) {
        return TemporalLayerRelation.compare(observerScale, targetScale).allowsAttackableGhost();
    }
}
