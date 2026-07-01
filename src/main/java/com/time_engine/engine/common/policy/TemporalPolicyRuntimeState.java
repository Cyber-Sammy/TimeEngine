package com.time_engine.engine.common.policy;

import com.time_engine.engine.common.intercept.TemporalInterceptManager;
import com.time_engine.engine.common.snapshot.SnapshotManager;

final class TemporalPolicyRuntimeState {
    private TemporalPolicyRuntimeState() {}

    static void resetAfterReload() {
        SnapshotManager.getInstance().clear();
        TemporalInterceptManager.getInstance().clear();
    }
}
