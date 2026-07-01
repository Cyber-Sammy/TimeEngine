package com.time_engine.engine.common.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemporalSessionTest {
    @Test
    void calculatesMinimumHistoryForDefaultSessionSettings() {
        assertEquals(180, session(200, 0.2F).minimumSnapshotHistoryTicks(20));
    }

    @Test
    void calculatesMinimumHistoryForLongSlowSession() {
        assertEquals(560, session(600, 0.1F).minimumSnapshotHistoryTicks(20));
    }

    @Test
    void retainsSafetyMarginAtNormalTimeScale() {
        assertEquals(20, session(600, 1.0F).minimumSnapshotHistoryTicks(20));
    }

    private static TemporalSession session(int durationTicks, float timeScale) {
        return new TemporalSession(
                UUID.randomUUID(), UUID.randomUUID(), 0, durationTicks, 20, timeScale, 32.0D);
    }
}
