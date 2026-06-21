package com.time_engine.config;

import com.time_engine.util.ModLog;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalConfigService {
    private TemporalConfigService() {}

    public static boolean canEdit(ServerPlayer player) {
        if (player.hasPermissions(2)) {
            return true;
        }
        return player.getServer().isSingleplayerOwner(player.getGameProfile());
    }

    public static ApplyResult apply(ServerPlayer player, TemporalConfigSnapshot requested) {
        if (!canEdit(player)) {
            return ApplyResult.failure("Server permission level 2 is required");
        }

        Optional<String> validationError = requested.validate();
        if (validationError.isPresent()) {
            return ApplyResult.failure(validationError.orElseThrow());
        }

        int previousHistoryTicks = TimeEngineConfig.snapshotHistoryTicks();
        TimeEngineConfig.DIAGNOSTIC_LOGGING.set(requested.diagnosticLogging());
        TimeEngineConfig.DURATION_TICKS.set(requested.durationTicks());
        TimeEngineConfig.COOLDOWN_TICKS.set(requested.cooldownTicks());
        TimeEngineConfig.TIME_SCALE.set(requested.timeScale());
        TimeEngineConfig.RADIUS.set(requested.radius());
        TimeEngineConfig.SNAPSHOT_HISTORY_TICKS.set(requested.snapshotHistoryTicks());
        TimeEngineConfig.MAX_TRACKED_ENTITIES_PER_SESSION.set(requested.maxTrackedEntities());
        TimeEngineConfig.SNAPSHOT_PLAYERS_ALWAYS.set(requested.snapshotPlayersAlways());
        TimeEngineConfig.GHOST_FRAME_INTERVAL_TICKS.set(requested.ghostFrameIntervalTicks());
        TimeEngineConfig.AFTERIMAGE_INTERVAL_TICKS.set(requested.afterimageIntervalTicks());
        TimeEngineConfig.AFTERIMAGE_LIFETIME_TICKS.set(requested.afterimageLifetimeTicks());
        TimeEngineConfig.AFTERIMAGE_OBSERVER_RADIUS.set(requested.afterimageObserverRadius());
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
                        ? "Configuration saved; snapshot history will be rebuilt"
                        : "Configuration saved";
        return ApplyResult.success(TemporalConfigSnapshot.current(), message);
    }

    public record ApplyResult(boolean success, TemporalConfigSnapshot snapshot, String message) {
        public static ApplyResult success(TemporalConfigSnapshot snapshot, String message) {
            return new ApplyResult(true, snapshot, message);
        }

        public static ApplyResult failure(String message) {
            return new ApplyResult(false, TemporalConfigSnapshot.current(), message);
        }
    }
}
