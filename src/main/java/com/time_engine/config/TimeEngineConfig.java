package com.time_engine.config;

import com.time_engine.common.temporal.TemporalConstants;
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
    public static final ModConfigSpec.IntValue GHOST_FRAME_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue AFTERIMAGE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue AFTERIMAGE_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue AFTERIMAGE_OBSERVER_RADIUS;
    public static final ModConfigSpec.DoubleValue PHANTOM_ATTACK_REACH;
    public static final ModConfigSpec.DoubleValue PHANTOM_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue PHANTOM_ATTACK_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue PHANTOM_ALLOWED_HIT_TICK_DRIFT;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        commonBuilder.push("logging");
        DIAGNOSTIC_LOGGING =
                commonBuilder
                        .comment("Enable diagnostic Time Engine lifecycle logs.")
                        .define(
                                "diagnosticLogging",
                                TemporalConfigSnapshot.defaults().diagnosticLogging());
        commonBuilder.pop();
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        serverBuilder.push("temporalSession");
        DURATION_TICKS =
                serverBuilder
                        .comment(
                                "Duration of a temporal session in server ticks. 20 ticks = 1 second.")
                        .defineInRange(
                                "durationTicks",
                                TemporalConstants.DEFAULT_DURATION_TICKS,
                                TemporalConfigSnapshot.MIN_DURATION_TICKS,
                                TemporalConfigSnapshot.MAX_DURATION_TICKS);
        COOLDOWN_TICKS =
                serverBuilder
                        .comment("Cooldown after a temporal session ends, in server ticks.")
                        .defineInRange(
                                "cooldownTicks",
                                TemporalConstants.DEFAULT_COOLDOWN_TICKS,
                                TemporalConfigSnapshot.MIN_COOLDOWN_TICKS,
                                TemporalConfigSnapshot.MAX_COOLDOWN_TICKS);
        TIME_SCALE =
                serverBuilder
                        .comment(
                                "Rate at which the perceived phantom timeline advances. Must be greater than 0 and at most 1.")
                        .defineInRange(
                                "timeScale",
                                (double) TemporalConstants.DEFAULT_TIME_SCALE,
                                TemporalConfigSnapshot.MIN_TIME_SCALE,
                                TemporalConfigSnapshot.MAX_TIME_SCALE);
        RADIUS =
                serverBuilder
                        .comment("Entity tracking radius for a temporal session, in blocks.")
                        .defineInRange(
                                "radius",
                                TemporalConstants.DEFAULT_RADIUS,
                                TemporalConfigSnapshot.MIN_RADIUS,
                                TemporalConfigSnapshot.MAX_RADIUS);
        serverBuilder.pop();

        serverBuilder.push("snapshots");
        SNAPSHOT_HISTORY_TICKS =
                serverBuilder
                        .comment(
                                "Length of retained entity history in server ticks. 20 ticks = 1 second.")
                        .defineInRange(
                                "historyTicks",
                                TemporalConfigSnapshot.defaults().snapshotHistoryTicks(),
                                TemporalConfigSnapshot.MIN_HISTORY_TICKS,
                                TemporalConfigSnapshot.MAX_HISTORY_TICKS);
        MAX_TRACKED_ENTITIES_PER_SESSION =
                serverBuilder
                        .comment("Maximum nearby entities captured per active temporal session.")
                        .defineInRange(
                                "maxTrackedEntitiesPerSession",
                                TemporalConfigSnapshot.defaults().maxTrackedEntities(),
                                TemporalConfigSnapshot.MIN_TRACKED_ENTITIES,
                                TemporalConfigSnapshot.MAX_TRACKED_ENTITIES);
        SNAPSHOT_PLAYERS_ALWAYS =
                serverBuilder
                        .comment(
                                "Keep bounded snapshot history for online players even when no temporal session is active.")
                        .define(
                                "snapshotPlayersAlways",
                                TemporalConfigSnapshot.defaults().snapshotPlayersAlways());
        serverBuilder.pop();

        serverBuilder.push("networking");
        GHOST_FRAME_INTERVAL_TICKS =
                serverBuilder
                        .comment(
                                "Interval between ghost frame packets in server ticks. Lower values are smoother but use more bandwidth.")
                        .defineInRange(
                                "ghostFrameIntervalTicks",
                                TemporalConfigSnapshot.defaults().ghostFrameIntervalTicks(),
                                TemporalConfigSnapshot.MIN_GHOST_FRAME_INTERVAL,
                                TemporalConfigSnapshot.MAX_GHOST_FRAME_INTERVAL);
        AFTERIMAGE_INTERVAL_TICKS =
                serverBuilder
                        .comment(
                                "Interval between afterimage anchors in server ticks. Lower values are smoother but use more bandwidth.")
                        .defineInRange(
                                "afterimageIntervalTicks",
                                TemporalConfigSnapshot.defaults().afterimageIntervalTicks(),
                                TemporalConfigSnapshot.MIN_AFTERIMAGE_INTERVAL,
                                TemporalConfigSnapshot.MAX_AFTERIMAGE_INTERVAL);
        AFTERIMAGE_LIFETIME_TICKS =
                serverBuilder
                        .comment("Lifetime of an afterimage anchor on observing clients, in ticks.")
                        .defineInRange(
                                "afterimageLifetimeTicks",
                                TemporalConfigSnapshot.defaults().afterimageLifetimeTicks(),
                                TemporalConfigSnapshot.MIN_AFTERIMAGE_LIFETIME,
                                TemporalConfigSnapshot.MAX_AFTERIMAGE_LIFETIME);
        AFTERIMAGE_OBSERVER_RADIUS =
                serverBuilder
                        .comment("Maximum distance at which players receive afterimage anchors.")
                        .defineInRange(
                                "afterimageObserverRadius",
                                TemporalConfigSnapshot.defaults().afterimageObserverRadius(),
                                TemporalConfigSnapshot.MIN_AFTERIMAGE_OBSERVER_RADIUS,
                                TemporalConfigSnapshot.MAX_AFTERIMAGE_OBSERVER_RADIUS);
        serverBuilder.pop();

        serverBuilder.push("phantomCombat");
        PHANTOM_ATTACK_REACH =
                serverBuilder
                        .comment("Maximum distance for server-validated phantom attacks.")
                        .defineInRange(
                                "phantomAttackReach",
                                TemporalConfigSnapshot.defaults().phantomAttackReach(),
                                TemporalConfigSnapshot.MIN_ATTACK_REACH,
                                TemporalConfigSnapshot.MAX_ATTACK_REACH);
        PHANTOM_DAMAGE_MULTIPLIER =
                serverBuilder
                        .comment("Multiplier applied to the attacker's base attack damage.")
                        .defineInRange(
                                "phantomDamageMultiplier",
                                TemporalConfigSnapshot.defaults().phantomDamageMultiplier(),
                                TemporalConfigSnapshot.MIN_DAMAGE_MULTIPLIER,
                                TemporalConfigSnapshot.MAX_DAMAGE_MULTIPLIER);
        PHANTOM_ATTACK_COOLDOWN_TICKS =
                serverBuilder
                        .comment("Minimum ticks between successful phantom attacks.")
                        .defineInRange(
                                "phantomAttackCooldownTicks",
                                TemporalConfigSnapshot.defaults().phantomAttackCooldownTicks(),
                                TemporalConfigSnapshot.MIN_ATTACK_COOLDOWN_TICKS,
                                TemporalConfigSnapshot.MAX_ATTACK_COOLDOWN_TICKS);
        PHANTOM_ALLOWED_HIT_TICK_DRIFT =
                serverBuilder
                        .comment(
                                "Maximum difference in perceived ticks between the client's rendered ghost frame and the server timeline.")
                        .defineInRange(
                                "phantomAllowedHitTickDrift",
                                TemporalConfigSnapshot.defaults().phantomAllowedHitTickDrift(),
                                TemporalConfigSnapshot.MIN_HIT_TICK_DRIFT,
                                TemporalConfigSnapshot.MAX_HIT_TICK_DRIFT);
        serverBuilder.pop();
        SERVER_SPEC = serverBuilder.build();
    }

    private TimeEngineConfig() {}

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
