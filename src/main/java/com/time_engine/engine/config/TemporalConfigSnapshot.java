package com.time_engine.engine.config;

import com.time_engine.engine.common.temporal.TemporalConstants;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

public record TemporalConfigSnapshot(
        boolean diagnosticLogging,
        int durationTicks,
        int cooldownTicks,
        double timeScale,
        double radius,
        int snapshotHistoryTicks,
        int maxTrackedEntities,
        boolean snapshotPlayersAlways,
        boolean trackNewEntitiesEnteringSessionRadius,
        int ghostFrameIntervalTicks,
        int afterimageIntervalTicks,
        int afterimageLifetimeTicks,
        double afterimageObserverRadius,
        boolean temporalInterceptEnabled,
        int maxTemporalBlocksPerSession,
        double maxInterceptCorrectionDistance,
        double phantomAttackReach,
        double phantomDamageMultiplier,
        int phantomAttackCooldownTicks,
        double phantomAllowedHitTickDrift) {
    public static final int MIN_DURATION_TICKS = 1;
    public static final int MAX_DURATION_TICKS = 20 * 60 * 10;
    public static final int MIN_COOLDOWN_TICKS = 0;
    public static final int MAX_COOLDOWN_TICKS = 20 * 60 * 60;
    public static final double MIN_TIME_SCALE = 0.01D;
    public static final double MAX_TIME_SCALE = 1.0D;
    public static final double MIN_RADIUS = 1.0D;
    public static final double MAX_RADIUS = 256.0D;
    public static final int MIN_HISTORY_TICKS = 20;
    public static final int MAX_HISTORY_TICKS = 20 * 60 * 10;
    public static final int MIN_TRACKED_ENTITIES = 1;
    public static final int MAX_TRACKED_ENTITIES = 2048;
    public static final int MIN_GHOST_FRAME_INTERVAL = 1;
    public static final int MAX_GHOST_FRAME_INTERVAL = 20;
    public static final int MIN_AFTERIMAGE_INTERVAL = 1;
    public static final int MAX_AFTERIMAGE_INTERVAL = 20;
    public static final int MIN_AFTERIMAGE_LIFETIME = 1;
    public static final int MAX_AFTERIMAGE_LIFETIME = 200;
    public static final double MIN_AFTERIMAGE_OBSERVER_RADIUS = 1.0D;
    public static final double MAX_AFTERIMAGE_OBSERVER_RADIUS = 256.0D;
    public static final int MIN_TEMPORAL_BLOCKS = 1;
    public static final int MAX_TEMPORAL_BLOCKS = 256;
    public static final double MIN_INTERCEPT_CORRECTION_DISTANCE = 1.0D;
    public static final double MAX_INTERCEPT_CORRECTION_DISTANCE = 64.0D;
    public static final double MIN_ATTACK_REACH = 1.0D;
    public static final double MAX_ATTACK_REACH = 16.0D;
    public static final double MIN_DAMAGE_MULTIPLIER = 0.0D;
    public static final double MAX_DAMAGE_MULTIPLIER = 10.0D;
    public static final int MIN_ATTACK_COOLDOWN_TICKS = 1;
    public static final int MAX_ATTACK_COOLDOWN_TICKS = 200;
    public static final double MIN_HIT_TICK_DRIFT = 0.0D;
    public static final double MAX_HIT_TICK_DRIFT = 20.0D;

    public static TemporalConfigSnapshot current() {
        return new TemporalConfigSnapshot(
                TimeEngineConfig.diagnosticLogging(),
                TimeEngineConfig.durationTicks(),
                TimeEngineConfig.cooldownTicks(),
                TimeEngineConfig.timeScale(),
                TimeEngineConfig.radius(),
                TimeEngineConfig.snapshotHistoryTicks(),
                TimeEngineConfig.maxTrackedEntitiesPerSession(),
                TimeEngineConfig.snapshotPlayersAlways(),
                TimeEngineConfig.trackNewEntitiesEnteringSessionRadius(),
                TimeEngineConfig.ghostFrameIntervalTicks(),
                TimeEngineConfig.afterimageIntervalTicks(),
                TimeEngineConfig.afterimageLifetimeTicks(),
                TimeEngineConfig.afterimageObserverRadius(),
                TimeEngineConfig.temporalInterceptEnabled(),
                TimeEngineConfig.maxTemporalBlocksPerSession(),
                TimeEngineConfig.maxInterceptCorrectionDistance(),
                TimeEngineConfig.phantomAttackReach(),
                TimeEngineConfig.phantomDamageMultiplier(),
                TimeEngineConfig.phantomAttackCooldownTicks(),
                TimeEngineConfig.phantomAllowedHitTickDrift());
    }

    public static TemporalConfigSnapshot defaults() {
        return new TemporalConfigSnapshot(
                false,
                TemporalConstants.DEFAULT_DURATION_TICKS,
                TemporalConstants.DEFAULT_COOLDOWN_TICKS,
                TemporalConstants.DEFAULT_TIME_SCALE,
                TemporalConstants.DEFAULT_RADIUS,
                20 * 10,
                128,
                true,
                true,
                2,
                2,
                12,
                64.0D,
                true,
                32,
                16.0D,
                4.5D,
                1.0D,
                10,
                3.0D);
    }

    public TemporalConfigSnapshot withSessionSettings(TemporalSessionSettings settings) {
        return new TemporalConfigSnapshot(
                diagnosticLogging,
                settings.durationTicks(),
                settings.cooldownTicks(),
                settings.timeScale(),
                settings.radius(),
                snapshotHistoryTicks,
                maxTrackedEntities,
                snapshotPlayersAlways,
                trackNewEntitiesEnteringSessionRadius,
                ghostFrameIntervalTicks,
                afterimageIntervalTicks,
                afterimageLifetimeTicks,
                afterimageObserverRadius,
                temporalInterceptEnabled,
                maxTemporalBlocksPerSession,
                maxInterceptCorrectionDistance,
                phantomAttackReach,
                phantomDamageMultiplier,
                phantomAttackCooldownTicks,
                phantomAllowedHitTickDrift);
    }

    public Optional<String> validate() {
        List<Optional<String>> validations =
                List.of(
                        validateRange(
                                "durationTicks",
                                durationTicks,
                                MIN_DURATION_TICKS,
                                MAX_DURATION_TICKS),
                        validateRange(
                                "cooldownTicks",
                                cooldownTicks,
                                MIN_COOLDOWN_TICKS,
                                MAX_COOLDOWN_TICKS),
                        validateRange("timeScale", timeScale, MIN_TIME_SCALE, MAX_TIME_SCALE),
                        validateRange("radius", radius, MIN_RADIUS, MAX_RADIUS),
                        validateRange(
                                "snapshotHistoryTicks",
                                snapshotHistoryTicks,
                                MIN_HISTORY_TICKS,
                                MAX_HISTORY_TICKS),
                        validateRange(
                                "maxTrackedEntities",
                                maxTrackedEntities,
                                MIN_TRACKED_ENTITIES,
                                MAX_TRACKED_ENTITIES),
                        validateRange(
                                "ghostFrameIntervalTicks",
                                ghostFrameIntervalTicks,
                                MIN_GHOST_FRAME_INTERVAL,
                                MAX_GHOST_FRAME_INTERVAL),
                        validateRange(
                                "afterimageIntervalTicks",
                                afterimageIntervalTicks,
                                MIN_AFTERIMAGE_INTERVAL,
                                MAX_AFTERIMAGE_INTERVAL),
                        validateRange(
                                "afterimageLifetimeTicks",
                                afterimageLifetimeTicks,
                                MIN_AFTERIMAGE_LIFETIME,
                                MAX_AFTERIMAGE_LIFETIME),
                        validateRange(
                                "afterimageObserverRadius",
                                afterimageObserverRadius,
                                MIN_AFTERIMAGE_OBSERVER_RADIUS,
                                MAX_AFTERIMAGE_OBSERVER_RADIUS),
                        validateRange(
                                "maxTemporalBlocksPerSession",
                                maxTemporalBlocksPerSession,
                                MIN_TEMPORAL_BLOCKS,
                                MAX_TEMPORAL_BLOCKS),
                        validateRange(
                                "maxInterceptCorrectionDistance",
                                maxInterceptCorrectionDistance,
                                MIN_INTERCEPT_CORRECTION_DISTANCE,
                                MAX_INTERCEPT_CORRECTION_DISTANCE),
                        validateRange(
                                "phantomAttackReach",
                                phantomAttackReach,
                                MIN_ATTACK_REACH,
                                MAX_ATTACK_REACH),
                        validateRange(
                                "phantomDamageMultiplier",
                                phantomDamageMultiplier,
                                MIN_DAMAGE_MULTIPLIER,
                                MAX_DAMAGE_MULTIPLIER),
                        validateRange(
                                "phantomAttackCooldownTicks",
                                phantomAttackCooldownTicks,
                                MIN_ATTACK_COOLDOWN_TICKS,
                                MAX_ATTACK_COOLDOWN_TICKS),
                        validateRange(
                                "phantomAllowedHitTickDrift",
                                phantomAllowedHitTickDrift,
                                MIN_HIT_TICK_DRIFT,
                                MAX_HIT_TICK_DRIFT));
        for (Optional<String> validation : validations) {
            if (validation.isPresent()) {
                return validation;
            }
        }
        return Optional.empty();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(diagnosticLogging);
        buffer.writeVarInt(durationTicks);
        buffer.writeVarInt(cooldownTicks);
        buffer.writeDouble(timeScale);
        buffer.writeDouble(radius);
        buffer.writeVarInt(snapshotHistoryTicks);
        buffer.writeVarInt(maxTrackedEntities);
        buffer.writeBoolean(snapshotPlayersAlways);
        buffer.writeBoolean(trackNewEntitiesEnteringSessionRadius);
        buffer.writeVarInt(ghostFrameIntervalTicks);
        buffer.writeVarInt(afterimageIntervalTicks);
        buffer.writeVarInt(afterimageLifetimeTicks);
        buffer.writeDouble(afterimageObserverRadius);
        buffer.writeBoolean(temporalInterceptEnabled);
        buffer.writeVarInt(maxTemporalBlocksPerSession);
        buffer.writeDouble(maxInterceptCorrectionDistance);
        buffer.writeDouble(phantomAttackReach);
        buffer.writeDouble(phantomDamageMultiplier);
        buffer.writeVarInt(phantomAttackCooldownTicks);
        buffer.writeDouble(phantomAllowedHitTickDrift);
    }

    public static TemporalConfigSnapshot decode(FriendlyByteBuf buffer) {
        return new TemporalConfigSnapshot(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readDouble());
    }

    private static Optional<String> validateRange(String name, int value, int min, int max) {
        String errorMessage = name + " must be between " + min + " and " + max;
        if (value < min) {
            return Optional.of(errorMessage);
        }
        if (value > max) {
            return Optional.of(errorMessage);
        }
        return Optional.empty();
    }

    private static Optional<String> validateRange(
            String name, double value, double min, double max) {
        String errorMessage = name + " must be between " + min + " and " + max;
        if (!Double.isFinite(value)) {
            return Optional.of(errorMessage);
        }
        if (value < min) {
            return Optional.of(errorMessage);
        }
        if (value > max) {
            return Optional.of(errorMessage);
        }
        return Optional.empty();
    }
}
