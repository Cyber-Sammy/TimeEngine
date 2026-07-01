package com.time_engine.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SnapshotHistoryGuidanceTest {
    @Test
    void recommendsHistoryFromDurationScaleAndSafetyMargin() {
        assertEquals(500, SnapshotHistoryGuidance.recommendedHistoryTicks(600, 0.2D));
    }

    @Test
    void normalTimeOnlyNeedsSafetyMargin() {
        assertEquals(20, SnapshotHistoryGuidance.recommendedHistoryTicks(600, 1.0D));
    }

    @Test
    void detectsShortHistory() {
        assertTrue(SnapshotHistoryGuidance.isHistoryTooShort(499, 600, 0.2D));
        assertFalse(SnapshotHistoryGuidance.isHistoryTooShort(500, 600, 0.2D));
    }
}
