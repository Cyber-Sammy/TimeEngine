package com.time_engine.api;

public record TemporalActivationResult(
        boolean success, TemporalActivationStatus status, int cooldownTicksRemaining) {
    public static TemporalActivationResult activated() {
        return new TemporalActivationResult(true, TemporalActivationStatus.ACTIVATED, 0);
    }

    public static TemporalActivationResult stopped() {
        return new TemporalActivationResult(true, TemporalActivationStatus.STOPPED, 0);
    }

    public static TemporalActivationResult cooldown(int cooldownTicksRemaining) {
        return new TemporalActivationResult(
                false, TemporalActivationStatus.COOLDOWN, cooldownTicksRemaining);
    }
}
