package com.time_engine.api;

import com.time_engine.engine.config.TemporalConfigService;
import com.time_engine.engine.config.TemporalSessionSettings;
import net.minecraft.server.level.ServerPlayer;

public record TemporalSessionOptions(
        int durationTicks, int cooldownTicks, float timeScale, double radius) {
    public TemporalSessionOptions {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("cooldownTicks must not be negative");
        }
        if (timeScale <= 0.0F || timeScale > 1.0F) {
            throw new IllegalArgumentException("timeScale must be in the range (0, 1]");
        }
        if (radius <= 0.0D) {
            throw new IllegalArgumentException("radius must be positive");
        }
    }

    public static TemporalSessionOptions fromCurrentPlayerSettings(ServerPlayer player) {
        return fromSettings(TemporalConfigService.sessionSettings(player));
    }

    public static TemporalSessionOptions fromSettings(TemporalSessionSettings settings) {
        return new TemporalSessionOptions(
                settings.durationTicks(),
                settings.cooldownTicks(),
                settings.timeScale(),
                settings.radius());
    }
}
