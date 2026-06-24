package com.time_engine.common.policy;

import com.time_engine.common.intercept.TemporalInterceptManager;
import com.time_engine.common.snapshot.SnapshotManager;

final class TemporalPolicyRuntimeState {
    private TemporalPolicyRuntimeState() {}

    static void resetAfterReload() {
        SnapshotManager.getInstance().clear();
        TemporalInterceptManager.getInstance().clear();
    }
}
