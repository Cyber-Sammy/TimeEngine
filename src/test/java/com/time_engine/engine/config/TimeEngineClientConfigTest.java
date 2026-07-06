package com.time_engine.engine.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeEngineClientConfigTest {
    @Test
    void debugOverlaysAreEnabledByDefaultForDevelopment() {
        assertTrue(TimeEngineClientConfig.DEFAULT_SHOW_GHOST_DEBUG_AABB);
        assertTrue(TimeEngineClientConfig.DEFAULT_SHOW_AFTERIMAGE_DEBUG_AABB);
        assertTrue(TimeEngineClientConfig.DEFAULT_SHOW_CAUSAL_PURSUIT_DEBUG_AABB);
        assertTrue(TimeEngineClientConfig.DEFAULT_SHOW_CAUSAL_DEBUG_HUD);
    }
}
