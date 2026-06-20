package com.time_engine.config;

import com.time_engine.common.temporal.TemporalConstants;
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
        int ghostFrameIntervalTicks,
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
                TimeEngineConfig.ghostFrameIntervalTicks(),
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
                2,
                4.5D,
                1.0D,
                10,
                3.0D);
    }

    public Optional<String> validate() {
        Optional<String> invalid;
        invalid =
                validateRange(
                        "durationTicks", durationTicks, MIN_DURATION_TICKS, MAX_DURATION_TICKS);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid =
                validateRange(
                        "cooldownTicks", cooldownTicks, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid = validateRange("timeScale", timeScale, MIN_TIME_SCALE, MAX_TIME_SCALE);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid = validateRange("radius", radius, MIN_RADIUS, MAX_RADIUS);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid =
                validateRange(
                        "snapshotHistoryTicks",
                        snapshotHistoryTicks,
                        MIN_HISTORY_TICKS,
                        MAX_HISTORY_TICKS);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid =
                validateRange(
                        "maxTrackedEntities",
                        maxTrackedEntities,
                        MIN_TRACKED_ENTITIES,
                        MAX_TRACKED_ENTITIES);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid =
                validateRange(
                        "ghostFrameIntervalTicks",
                        ghostFrameIntervalTicks,
                        MIN_GHOST_FRAME_INTERVAL,
                        MAX_GHOST_FRAME_INTERVAL);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid =
                validateRange(
                        "phantomAttackReach",
                        phantomAttackReach,
                        MIN_ATTACK_REACH,
                        MAX_ATTACK_REACH);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid =
                validateRange(
                        "phantomDamageMultiplier",
                        phantomDamageMultiplier,
                        MIN_DAMAGE_MULTIPLIER,
                        MAX_DAMAGE_MULTIPLIER);
        if (invalid.isPresent()) {
            return invalid;
        }
        invalid =
                validateRange(
                        "phantomAttackCooldownTicks",
                        phantomAttackCooldownTicks,
                        MIN_ATTACK_COOLDOWN_TICKS,
                        MAX_ATTACK_COOLDOWN_TICKS);
        if (invalid.isPresent()) {
            return invalid;
        }
        return validateRange(
                "phantomAllowedHitTickDrift",
                phantomAllowedHitTickDrift,
                MIN_HIT_TICK_DRIFT,
                MAX_HIT_TICK_DRIFT);
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
        buffer.writeVarInt(ghostFrameIntervalTicks);
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
                buffer.readVarInt(),
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
