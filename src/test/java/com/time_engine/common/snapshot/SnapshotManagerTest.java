package com.time_engine.common.snapshot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapshotManagerTest {
    @Test
    void recommendsHistoryForDefaultSessionSettings() {
        assertEquals(180, SnapshotManager.recommendedHistoryTicks(200, 0.2D));
    }

    @Test
    void recommendsHistoryForLongSlowSession() {
        assertEquals(560, SnapshotManager.recommendedHistoryTicks(600, 0.1D));
    }

    @Test
    void retainsSafetyMarginAtNormalTimeScale() {
        assertEquals(20, SnapshotManager.recommendedHistoryTicks(600, 1.0D));
    }
}
