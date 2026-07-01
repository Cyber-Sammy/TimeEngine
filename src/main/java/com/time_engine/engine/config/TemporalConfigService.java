package com.time_engine.engine.config;

import com.time_engine.engine.util.ModLog;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalConfigService {
    private static final Map<UUID, TemporalSessionSettings> SESSION_OVERRIDES = new HashMap<>();

    private TemporalConfigService() {}

    public static boolean canEdit(ServerPlayer player) {
        if (player.hasPermissions(2)) {
            return true;
        }
        return player.getServer().isSingleplayerOwner(player.getGameProfile());
    }

    public static ApplyResult apply(ServerPlayer player, TemporalConfigSnapshot requested) {
        if (!canEdit(player)) {
            return ApplyResult.failure(currentFor(player), "Server permission level 2 is required");
        }

        Optional<String> validationError = requested.validate();
        if (validationError.isPresent()) {
            return ApplyResult.failure(currentFor(player), validationError.orElseThrow());
        }

        int previousHistoryTicks = TimeEngineConfig.snapshotHistoryTicks();
        SESSION_OVERRIDES.put(player.getUUID(), TemporalSessionSettings.fromSnapshot(requested));
        TimeEngineConfig.DIAGNOSTIC_LOGGING.set(requested.diagnosticLogging());
        TimeEngineConfig.SNAPSHOT_HISTORY_TICKS.set(requested.snapshotHistoryTicks());
        TimeEngineConfig.MAX_TRACKED_ENTITIES_PER_SESSION.set(requested.maxTrackedEntities());
        TimeEngineConfig.SNAPSHOT_PLAYERS_ALWAYS.set(requested.snapshotPlayersAlways());
        TimeEngineConfig.TRACK_NEW_ENTITIES_ENTERING_SESSION_RADIUS.set(
                requested.trackNewEntitiesEnteringSessionRadius());
        TimeEngineConfig.GHOST_FRAME_INTERVAL_TICKS.set(requested.ghostFrameIntervalTicks());
        TimeEngineConfig.AFTERIMAGE_INTERVAL_TICKS.set(requested.afterimageIntervalTicks());
        TimeEngineConfig.AFTERIMAGE_LIFETIME_TICKS.set(requested.afterimageLifetimeTicks());
        TimeEngineConfig.AFTERIMAGE_OBSERVER_RADIUS.set(requested.afterimageObserverRadius());
        TimeEngineConfig.TEMPORAL_INTERCEPT_ENABLED.set(requested.temporalInterceptEnabled());
        TimeEngineConfig.MAX_TEMPORAL_BLOCKS_PER_SESSION.set(
                requested.maxTemporalBlocksPerSession());
        TimeEngineConfig.MAX_INTERCEPT_CORRECTION_DISTANCE.set(
                requested.maxInterceptCorrectionDistance());
        TimeEngineConfig.PHANTOM_ATTACK_REACH.set(requested.phantomAttackReach());
        TimeEngineConfig.PHANTOM_DAMAGE_MULTIPLIER.set(requested.phantomDamageMultiplier());
        TimeEngineConfig.PHANTOM_ATTACK_COOLDOWN_TICKS.set(requested.phantomAttackCooldownTicks());
        TimeEngineConfig.PHANTOM_ALLOWED_HIT_TICK_DRIFT.set(requested.phantomAllowedHitTickDrift());
        TimeEngineConfig.COMMON_SPEC.save();
        TimeEngineConfig.SERVER_SPEC.save();

        boolean historyReset = previousHistoryTicks != requested.snapshotHistoryTicks();
        ModLog.diagnostic(
                "Player {} updated runtime configuration (snapshotHistoryReset={})",
                player.getUUID(),
                historyReset);
        String message =
                historyReset
                        ? "Configuration saved; player session settings updated; snapshot history will be rebuilt"
                        : "Configuration saved; player session settings updated";
        return ApplyResult.success(currentFor(player), message);
    }

    public static TemporalConfigSnapshot currentFor(ServerPlayer player) {
        return TemporalConfigSnapshot.current().withSessionSettings(sessionSettings(player));
    }

    public static TemporalSessionSettings sessionSettings(ServerPlayer player) {
        return SESSION_OVERRIDES.getOrDefault(
                player.getUUID(), TemporalSessionSettings.currentDefaults());
    }

    public record ApplyResult(boolean success, TemporalConfigSnapshot snapshot, String message) {
        public static ApplyResult success(TemporalConfigSnapshot snapshot, String message) {
            return new ApplyResult(true, snapshot, message);
        }

        public static ApplyResult failure(TemporalConfigSnapshot snapshot, String message) {
            return new ApplyResult(false, snapshot, message);
        }
    }
}
