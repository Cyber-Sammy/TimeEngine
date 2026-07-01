package com.time_engine.engine.config;

public record TemporalSessionSettings(
        int durationTicks, int cooldownTicks, float timeScale, double radius) {
    public static TemporalSessionSettings currentDefaults() {
        return new TemporalSessionSettings(
                TimeEngineConfig.durationTicks(),
                TimeEngineConfig.cooldownTicks(),
                TimeEngineConfig.timeScale(),
                TimeEngineConfig.radius());
    }

    public static TemporalSessionSettings fromSnapshot(TemporalConfigSnapshot snapshot) {
        return new TemporalSessionSettings(
                snapshot.durationTicks(),
                snapshot.cooldownTicks(),
                (float) snapshot.timeScale(),
                snapshot.radius());
    }
}
