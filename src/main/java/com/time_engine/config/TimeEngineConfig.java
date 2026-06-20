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
    public static final ModConfigSpec.DoubleValue PHANTOM_ATTACK_REACH;
    public static final ModConfigSpec.DoubleValue PHANTOM_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue PHANTOM_ATTACK_COOLDOWN_TICKS;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        commonBuilder.push("logging");
        DIAGNOSTIC_LOGGING =
                commonBuilder
                        .comment("Enable diagnostic Time Engine lifecycle logs.")
                        .define("diagnosticLogging", false);
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
                                1,
                                20 * 60 * 10);
        COOLDOWN_TICKS =
                serverBuilder
                        .comment("Cooldown after a temporal session ends, in server ticks.")
                        .defineInRange(
                                "cooldownTicks",
                                TemporalConstants.DEFAULT_COOLDOWN_TICKS,
                                0,
                                20 * 60 * 60);
        TIME_SCALE =
                serverBuilder
                        .comment(
                                "Rate at which the perceived phantom timeline advances. Must be greater than 0 and at most 1.")
                        .defineInRange(
                                "timeScale",
                                (double) TemporalConstants.DEFAULT_TIME_SCALE,
                                0.01D,
                                1.0D);
        RADIUS =
                serverBuilder
                        .comment("Entity tracking radius for a temporal session, in blocks.")
                        .defineInRange("radius", TemporalConstants.DEFAULT_RADIUS, 1.0D, 256.0D);
        serverBuilder.pop();

        serverBuilder.push("snapshots");
        SNAPSHOT_HISTORY_TICKS =
                serverBuilder
                        .comment(
                                "Length of retained entity history in server ticks. 20 ticks = 1 second.")
                        .defineInRange("historyTicks", 20 * 10, 20, 20 * 60 * 10);
        MAX_TRACKED_ENTITIES_PER_SESSION =
                serverBuilder
                        .comment("Maximum nearby entities captured per active temporal session.")
                        .defineInRange("maxTrackedEntitiesPerSession", 128, 1, 2048);
        SNAPSHOT_PLAYERS_ALWAYS =
                serverBuilder
                        .comment(
                                "Keep bounded snapshot history for online players even when no temporal session is active.")
                        .define("snapshotPlayersAlways", true);
        serverBuilder.pop();

        serverBuilder.push("networking");
        GHOST_FRAME_INTERVAL_TICKS =
                serverBuilder
                        .comment(
                                "Interval between ghost frame packets in server ticks. Lower values are smoother but use more bandwidth.")
                        .defineInRange("ghostFrameIntervalTicks", 2, 1, 20);
        serverBuilder.pop();

        serverBuilder.push("phantomCombat");
        PHANTOM_ATTACK_REACH =
                serverBuilder
                        .comment("Maximum distance for server-validated phantom attacks.")
                        .defineInRange("phantomAttackReach", 4.5D, 1.0D, 16.0D);
        PHANTOM_DAMAGE_MULTIPLIER =
                serverBuilder
                        .comment("Multiplier applied to the attacker's base attack damage.")
                        .defineInRange("phantomDamageMultiplier", 1.0D, 0.0D, 10.0D);
        PHANTOM_ATTACK_COOLDOWN_TICKS =
                serverBuilder
                        .comment("Minimum ticks between successful phantom attacks.")
                        .defineInRange("phantomAttackCooldownTicks", 10, 1, 200);
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

    public static double phantomAttackReach() {
        return PHANTOM_ATTACK_REACH.get();
    }

    public static float phantomDamageMultiplier() {
        return PHANTOM_DAMAGE_MULTIPLIER.get().floatValue();
    }

    public static int phantomAttackCooldownTicks() {
        return PHANTOM_ATTACK_COOLDOWN_TICKS.get();
    }
}
