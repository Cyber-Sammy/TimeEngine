package com.time_engine.engine.common.intercept;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TemporalInterceptRulesTest {
    @Test
    void allowsInterceptWhenObserverIsFasterThanTarget() {
        assertTrue(TemporalInterceptRules.allowsIntercept(0.2D, 0.5D));
        assertTrue(TemporalInterceptRules.allowsIntercept(0.5D, 1.0D));
    }

    @Test
    void rejectsInterceptAtEqualScale() {
        assertFalse(TemporalInterceptRules.allowsIntercept(0.2D, 0.2D));
    }

    @Test
    void rejectsInterceptWhenTargetIsFasterThanObserver() {
        assertFalse(TemporalInterceptRules.allowsIntercept(0.5D, 0.2D));
    }
}
