package com.time_engine.sandevistan.client;

import com.time_engine.api.client.TemporalClientApi;

public final class SandevistanClientActivation {
    private SandevistanClientActivation() {}

    public static void requestToggle() {
        TemporalClientApi.requestToggle();
    }
}
