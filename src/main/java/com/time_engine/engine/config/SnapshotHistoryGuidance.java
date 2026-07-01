package com.time_engine.engine.config;

import com.time_engine.engine.common.temporal.TemporalConstants;

public final class SnapshotHistoryGuidance {
    private SnapshotHistoryGuidance() {}

    public static int recommendedHistoryTicks(int durationTicks, double timeScale) {
        double clampedScale =
                Math.clamp(
                        timeScale,
                        TemporalConfigSnapshot.MIN_TIME_SCALE,
                        TemporalConfigSnapshot.MAX_TIME_SCALE);
        int maximumPerceptionDelay = (int) Math.ceil(durationTicks * (1.0D - clampedScale));
        return maximumPerceptionDelay + TemporalConstants.SNAPSHOT_HISTORY_SAFETY_MARGIN_TICKS;
    }

    public static boolean isHistoryTooShort(int historyTicks, int durationTicks, double timeScale) {
        return historyTicks < recommendedHistoryTicks(durationTicks, timeScale);
    }
}
