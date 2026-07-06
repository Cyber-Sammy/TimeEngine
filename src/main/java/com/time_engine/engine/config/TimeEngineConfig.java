package com.time_engine.engine.config;

import com.time_engine.engine.common.temporal.TemporalConstants;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class TimeEngineConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.BooleanValue DIAGNOSTIC_LOGGING;
    public static final ModConfigSpec.IntValue DURATION_TICKS;
    public static final ModConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue TIME_SCALE;
    public static final ModConfigSpec.DoubleValue RADIUS;
    public static final ModConfigSpec.IntValue SNAPSHOT_HISTORY_TICKS;
    public static final ModConfigSpec.IntValue MAX_TRACKED_ENTITIES_PER_SESSION;
    public static final ModConfigSpec.BooleanValue SNAPSHOT_PLAYERS_ALWAYS;
    public static final ModConfigSpec.BooleanValue TRACK_NEW_ENTITIES_ENTERING_SESSION_RADIUS;
    public static final ModConfigSpec.IntValue GHOST_FRAME_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue AFTERIMAGE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue AFTERIMAGE_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue AFTERIMAGE_OBSERVER_RADIUS;
    public static final ModConfigSpec.BooleanValue TEMPORAL_INTERCEPT_ENABLED;
    public static final ModConfigSpec.IntValue MAX_TEMPORAL_BLOCKS_PER_SESSION;
    public static final ModConfigSpec.DoubleValue MAX_INTERCEPT_CORRECTION_DISTANCE;
    public static final ModConfigSpec.DoubleValue PHANTOM_ATTACK_REACH;
    public static final ModConfigSpec.DoubleValue PHANTOM_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue PHANTOM_ATTACK_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue PHANTOM_ALLOWED_HIT_TICK_DRIFT;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        DIAGNOSTIC_LOGGING = registerLoggingConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        SessionConfig sessionConfig = registerSessionConfig(serverBuilder);
        SnapshotConfig snapshotConfig = registerSnapshotConfig(serverBuilder);
        NetworkingConfig networkingConfig = registerNetworkingConfig(serverBuilder);
        InterceptConfig interceptConfig = registerInterceptConfig(serverBuilder);
        CombatConfig combatConfig = registerCombatConfig(serverBuilder);

        DURATION_TICKS = sessionConfig.durationTicks();
        COOLDOWN_TICKS = sessionConfig.cooldownTicks();
        TIME_SCALE = sessionConfig.timeScale();
        RADIUS = sessionConfig.radius();
        SNAPSHOT_HISTORY_TICKS = snapshotConfig.historyTicks();
        MAX_TRACKED_ENTITIES_PER_SESSION = snapshotConfig.maxTrackedEntitiesPerSession();
        SNAPSHOT_PLAYERS_ALWAYS = snapshotConfig.snapshotPlayersAlways();
        TRACK_NEW_ENTITIES_ENTERING_SESSION_RADIUS =
                snapshotConfig.trackNewEntitiesEnteringSessionRadius();
        GHOST_FRAME_INTERVAL_TICKS = networkingConfig.ghostFrameIntervalTicks();
        AFTERIMAGE_INTERVAL_TICKS = networkingConfig.afterimageIntervalTicks();
        AFTERIMAGE_LIFETIME_TICKS = networkingConfig.afterimageLifetimeTicks();
        AFTERIMAGE_OBSERVER_RADIUS = networkingConfig.afterimageObserverRadius();
        TEMPORAL_INTERCEPT_ENABLED = interceptConfig.temporalInterceptEnabled();
        MAX_TEMPORAL_BLOCKS_PER_SESSION = interceptConfig.maxTemporalBlocksPerSession();
        MAX_INTERCEPT_CORRECTION_DISTANCE = interceptConfig.maxInterceptCorrectionDistance();
        PHANTOM_ATTACK_REACH = combatConfig.phantomAttackReach();
        PHANTOM_DAMAGE_MULTIPLIER = combatConfig.phantomDamageMultiplier();
        PHANTOM_ATTACK_COOLDOWN_TICKS = combatConfig.phantomAttackCooldownTicks();
        PHANTOM_ALLOWED_HIT_TICK_DRIFT = combatConfig.phantomAllowedHitTickDrift();
        SERVER_SPEC = serverBuilder.build();
    }

    private TimeEngineConfig() {}

    private static ModConfigSpec.BooleanValue registerLoggingConfig(
            ModConfigSpec.Builder commonBuilder) {
        commonBuilder.push("logging");
        ModConfigSpec.BooleanValue diagnosticLogging =
                commonBuilder
                        .comment("Enable diagnostic Time Engine lifecycle logs.")
                        .define(
                                "diagnosticLogging",
                                TemporalConfigSnapshot.defaults().diagnosticLogging());
        commonBuilder.pop();
        return diagnosticLogging;
    }

    private static SessionConfig registerSessionConfig(ModConfigSpec.Builder serverBuilder) {
        serverBuilder.push("temporalSession");
        ModConfigSpec.IntValue durationTicks =
                serverBuilder
                        .comment(
                                "Duration of a temporal session in server ticks. 20 ticks = 1 second.")
                        .defineInRange(
                                "durationTicks",
                                TemporalConstants.DEFAULT_DURATION_TICKS,
                                TemporalConfigSnapshot.MIN_DURATION_TICKS,
                                TemporalConfigSnapshot.MAX_DURATION_TICKS);
        ModConfigSpec.IntValue cooldownTicks =
                serverBuilder
                        .comment("Cooldown after a temporal session ends, in server ticks.")
                        .defineInRange(
                                "cooldownTicks",
                                TemporalConstants.DEFAULT_COOLDOWN_TICKS,
                                TemporalConfigSnapshot.MIN_COOLDOWN_TICKS,
                                TemporalConfigSnapshot.MAX_COOLDOWN_TICKS);
        ModConfigSpec.DoubleValue timeScale =
                serverBuilder
                        .comment(
                                "Rate at which the perceived phantom timeline advances. Must be greater than 0 and at most 1.")
                        .defineInRange(
                                "timeScale",
                                (double) TemporalConstants.DEFAULT_TIME_SCALE,
                                TemporalConfigSnapshot.MIN_TIME_SCALE,
                                TemporalConfigSnapshot.MAX_TIME_SCALE);
        ModConfigSpec.DoubleValue radius =
                serverBuilder
                        .comment("Entity tracking radius for a temporal session, in blocks.")
                        .defineInRange(
                                "radius",
                                TemporalConstants.DEFAULT_RADIUS,
                                TemporalConfigSnapshot.MIN_RADIUS,
                                TemporalConfigSnapshot.MAX_RADIUS);
        serverBuilder.pop();
        return new SessionConfig(durationTicks, cooldownTicks, timeScale, radius);
    }

    private static SnapshotConfig registerSnapshotConfig(ModConfigSpec.Builder serverBuilder) {
        serverBuilder.push("snapshots");
        ModConfigSpec.IntValue historyTicks =
                serverBuilder
                        .comment(
                                "Length of retained entity history in server ticks. 20 ticks = 1 second.")
                        .defineInRange(
                                "historyTicks",
                                TemporalConfigSnapshot.defaults().snapshotHistoryTicks(),
                                TemporalConfigSnapshot.MIN_HISTORY_TICKS,
                                TemporalConfigSnapshot.MAX_HISTORY_TICKS);
        ModConfigSpec.IntValue maxTrackedEntitiesPerSession =
                serverBuilder
                        .comment("Maximum nearby entities captured per active temporal session.")
                        .defineInRange(
                                "maxTrackedEntitiesPerSession",
                                TemporalConfigSnapshot.defaults().maxTrackedEntities(),
                                TemporalConfigSnapshot.MIN_TRACKED_ENTITIES,
                                TemporalConfigSnapshot.MAX_TRACKED_ENTITIES);
        ModConfigSpec.BooleanValue snapshotPlayersAlways =
                serverBuilder
                        .comment(
                                "Keep bounded snapshot history for online players even when no temporal session is active.")
                        .define(
                                "snapshotPlayersAlways",
                                TemporalConfigSnapshot.defaults().snapshotPlayersAlways());
        ModConfigSpec.BooleanValue trackNewEntitiesEnteringSessionRadius =
                serverBuilder
                        .comment(
                                "When enabled, eligible entities that enter an active session radius begin receiving snapshot history. Disable to keep each session limited to entities admitted when the session first scans.")
                        .define(
                                "trackNewEntitiesEnteringSessionRadius",
                                TemporalConfigSnapshot.defaults()
                                        .trackNewEntitiesEnteringSessionRadius());
        serverBuilder.pop();
        return new SnapshotConfig(
                historyTicks,
                maxTrackedEntitiesPerSession,
                snapshotPlayersAlways,
                trackNewEntitiesEnteringSessionRadius);
    }

    private static NetworkingConfig registerNetworkingConfig(ModConfigSpec.Builder serverBuilder) {
        serverBuilder.push("networking");
        ModConfigSpec.IntValue ghostFrameIntervalTicks =
                serverBuilder
                        .comment(
                                "Interval between ghost frame packets in server ticks. Lower values are smoother but use more bandwidth.")
                        .defineInRange(
                                "ghostFrameIntervalTicks",
                                TemporalConfigSnapshot.defaults().ghostFrameIntervalTicks(),
                                TemporalConfigSnapshot.MIN_GHOST_FRAME_INTERVAL,
                                TemporalConfigSnapshot.MAX_GHOST_FRAME_INTERVAL);
        ModConfigSpec.IntValue afterimageIntervalTicks =
                serverBuilder
                        .comment(
                                "Interval between afterimage anchors in server ticks. Lower values are smoother but use more bandwidth.")
                        .defineInRange(
                                "afterimageIntervalTicks",
                                TemporalConfigSnapshot.defaults().afterimageIntervalTicks(),
                                TemporalConfigSnapshot.MIN_AFTERIMAGE_INTERVAL,
                                TemporalConfigSnapshot.MAX_AFTERIMAGE_INTERVAL);
        ModConfigSpec.IntValue afterimageLifetimeTicks =
                serverBuilder
                        .comment("Lifetime of an afterimage anchor on observing clients, in ticks.")
                        .defineInRange(
                                "afterimageLifetimeTicks",
                                TemporalConfigSnapshot.defaults().afterimageLifetimeTicks(),
                                TemporalConfigSnapshot.MIN_AFTERIMAGE_LIFETIME,
                                TemporalConfigSnapshot.MAX_AFTERIMAGE_LIFETIME);
        ModConfigSpec.DoubleValue afterimageObserverRadius =
                serverBuilder
                        .comment("Maximum distance at which players receive afterimage anchors.")
                        .defineInRange(
                                "afterimageObserverRadius",
                                TemporalConfigSnapshot.defaults().afterimageObserverRadius(),
                                TemporalConfigSnapshot.MIN_AFTERIMAGE_OBSERVER_RADIUS,
                                TemporalConfigSnapshot.MAX_AFTERIMAGE_OBSERVER_RADIUS);
        serverBuilder.pop();
        return new NetworkingConfig(
                ghostFrameIntervalTicks,
                afterimageIntervalTicks,
                afterimageLifetimeTicks,
                afterimageObserverRadius);
    }

    private static InterceptConfig registerInterceptConfig(ModConfigSpec.Builder serverBuilder) {
        serverBuilder.push("temporalIntercept");
        ModConfigSpec.BooleanValue temporalInterceptEnabled =
                serverBuilder
                        .comment("Enable limited server-authoritative temporal block intercepts.")
                        .define(
                                "enabled",
                                TemporalConfigSnapshot.defaults().temporalInterceptEnabled());
        ModConfigSpec.IntValue maxTemporalBlocksPerSession =
                serverBuilder
                        .comment("Maximum placed temporal obstacles retained per active session.")
                        .defineInRange(
                                "maxBlocksPerSession",
                                TemporalConfigSnapshot.defaults().maxTemporalBlocksPerSession(),
                                TemporalConfigSnapshot.MIN_TEMPORAL_BLOCKS,
                                TemporalConfigSnapshot.MAX_TEMPORAL_BLOCKS);
        ModConfigSpec.DoubleValue maxInterceptCorrectionDistance =
                serverBuilder
                        .comment(
                                "Maximum distance a real entity may be position-corrected by an intercept.")
                        .defineInRange(
                                "maxCorrectionDistance",
                                TemporalConfigSnapshot.defaults().maxInterceptCorrectionDistance(),
                                TemporalConfigSnapshot.MIN_INTERCEPT_CORRECTION_DISTANCE,
                                TemporalConfigSnapshot.MAX_INTERCEPT_CORRECTION_DISTANCE);
        serverBuilder.pop();
        return new InterceptConfig(
                temporalInterceptEnabled,
                maxTemporalBlocksPerSession,
                maxInterceptCorrectionDistance);
    }

    private static CombatConfig registerCombatConfig(ModConfigSpec.Builder serverBuilder) {
        serverBuilder.push("phantomCombat");
        ModConfigSpec.DoubleValue phantomAttackReach =
                serverBuilder
                        .comment("Maximum distance for server-validated phantom attacks.")
                        .defineInRange(
                                "phantomAttackReach",
                                TemporalConfigSnapshot.defaults().phantomAttackReach(),
                                TemporalConfigSnapshot.MIN_ATTACK_REACH,
                                TemporalConfigSnapshot.MAX_ATTACK_REACH);
        ModConfigSpec.DoubleValue phantomDamageMultiplier =
                serverBuilder
                        .comment("Multiplier applied to the attacker's base attack damage.")
                        .defineInRange(
                                "phantomDamageMultiplier",
                                TemporalConfigSnapshot.defaults().phantomDamageMultiplier(),
                                TemporalConfigSnapshot.MIN_DAMAGE_MULTIPLIER,
                                TemporalConfigSnapshot.MAX_DAMAGE_MULTIPLIER);
        ModConfigSpec.IntValue phantomAttackCooldownTicks =
                serverBuilder
                        .comment("Minimum ticks between successful phantom attacks.")
                        .defineInRange(
                                "phantomAttackCooldownTicks",
                                TemporalConfigSnapshot.defaults().phantomAttackCooldownTicks(),
                                TemporalConfigSnapshot.MIN_ATTACK_COOLDOWN_TICKS,
                                TemporalConfigSnapshot.MAX_ATTACK_COOLDOWN_TICKS);
        ModConfigSpec.DoubleValue phantomAllowedHitTickDrift =
                serverBuilder
                        .comment(
                                "Maximum difference in perceived ticks between the client's rendered ghost frame and the server timeline.")
                        .defineInRange(
                                "phantomAllowedHitTickDrift",
                                TemporalConfigSnapshot.defaults().phantomAllowedHitTickDrift(),
                                TemporalConfigSnapshot.MIN_HIT_TICK_DRIFT,
                                TemporalConfigSnapshot.MAX_HIT_TICK_DRIFT);
        serverBuilder.pop();
        return new CombatConfig(
                phantomAttackReach,
                phantomDamageMultiplier,
                phantomAttackCooldownTicks,
                phantomAllowedHitTickDrift);
    }

    private record SessionConfig(
            ModConfigSpec.IntValue durationTicks,
            ModConfigSpec.IntValue cooldownTicks,
            ModConfigSpec.DoubleValue timeScale,
            ModConfigSpec.DoubleValue radius) {}

    private record SnapshotConfig(
            ModConfigSpec.IntValue historyTicks,
            ModConfigSpec.IntValue maxTrackedEntitiesPerSession,
            ModConfigSpec.BooleanValue snapshotPlayersAlways,
            ModConfigSpec.BooleanValue trackNewEntitiesEnteringSessionRadius) {}

    private record NetworkingConfig(
            ModConfigSpec.IntValue ghostFrameIntervalTicks,
            ModConfigSpec.IntValue afterimageIntervalTicks,
            ModConfigSpec.IntValue afterimageLifetimeTicks,
            ModConfigSpec.DoubleValue afterimageObserverRadius) {}

    private record InterceptConfig(
            ModConfigSpec.BooleanValue temporalInterceptEnabled,
            ModConfigSpec.IntValue maxTemporalBlocksPerSession,
            ModConfigSpec.DoubleValue maxInterceptCorrectionDistance) {}

    private record CombatConfig(
            ModConfigSpec.DoubleValue phantomAttackReach,
            ModConfigSpec.DoubleValue phantomDamageMultiplier,
            ModConfigSpec.IntValue phantomAttackCooldownTicks,
            ModConfigSpec.DoubleValue phantomAllowedHitTickDrift) {}

    public static boolean diagnosticLogging() {
        return DIAGNOSTIC_LOGGING.get();
    }

    public static int durationTicks() {
        return DURATION_TICKS.get();
    }

    public static int cooldownTicks() {
        return COOLDOWN_TICKS.get();
    }

    public static float timeScale() {
        return TIME_SCALE.get().floatValue();
    }

    public static double radius() {
        return RADIUS.get();
    }

    public static int snapshotHistoryTicks() {
        return SNAPSHOT_HISTORY_TICKS.get();
    }

    public static int maxTrackedEntitiesPerSession() {
        return MAX_TRACKED_ENTITIES_PER_SESSION.get();
    }

    public static boolean snapshotPlayersAlways() {
        return SNAPSHOT_PLAYERS_ALWAYS.get();
    }

    public static boolean trackNewEntitiesEnteringSessionRadius() {
        return TRACK_NEW_ENTITIES_ENTERING_SESSION_RADIUS.get();
    }

    public static int ghostFrameIntervalTicks() {
        return GHOST_FRAME_INTERVAL_TICKS.get();
    }

    public static int afterimageIntervalTicks() {
        return AFTERIMAGE_INTERVAL_TICKS.get();
    }

    public static int afterimageLifetimeTicks() {
        return AFTERIMAGE_LIFETIME_TICKS.get();
    }

    public static double afterimageObserverRadius() {
        return AFTERIMAGE_OBSERVER_RADIUS.get();
    }

    public static boolean temporalInterceptEnabled() {
        return TEMPORAL_INTERCEPT_ENABLED.get();
    }

    public static int maxTemporalBlocksPerSession() {
        return MAX_TEMPORAL_BLOCKS_PER_SESSION.get();
    }

    public static double maxInterceptCorrectionDistance() {
        return MAX_INTERCEPT_CORRECTION_DISTANCE.get();
    }

    public static double phantomAttackReach() {
        return PHANTOM_ATTACK_REACH.get();
    }

    public static float phantomDamageMultiplier() {
        return PHANTOM_DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static int phantomAttackCooldownTicks() {
        return PHANTOM_ATTACK_COOLDOWN_TICKS.get();
    }

    public static double phantomAllowedHitTickDrift() {
        return PHANTOM_ALLOWED_HIT_TICK_DRIFT.get();
    }
}
