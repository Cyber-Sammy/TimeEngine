package com.time_engine.engine.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TimeEngineClientConfig {
    public static final boolean DEFAULT_SHOW_GHOST_DEBUG_AABB = true;
    public static final boolean DEFAULT_SHOW_AFTERIMAGE_DEBUG_AABB = true;
    public static final boolean DEFAULT_SHOW_CAUSAL_PURSUIT_DEBUG_AABB = true;
    public static final boolean DEFAULT_SHOW_CAUSAL_DEBUG_HUD = true;

    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_GHOST_DEBUG_AABB;
    public static final ModConfigSpec.BooleanValue SHOW_AFTERIMAGE_DEBUG_AABB;
    public static final ModConfigSpec.BooleanValue SHOW_CAUSAL_PURSUIT_DEBUG_AABB;
    public static final ModConfigSpec.BooleanValue SHOW_CAUSAL_DEBUG_HUD;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("debugOverlays");
        SHOW_GHOST_DEBUG_AABB =
                builder.comment("Render cyan historical ghost AABB debug boxes.")
                        .define("showGhostDebugAabb", DEFAULT_SHOW_GHOST_DEBUG_AABB);
        SHOW_AFTERIMAGE_DEBUG_AABB =
                builder.comment("Render purple afterimage AABB debug boxes.")
                        .define("showAfterimageDebugAabb", DEFAULT_SHOW_AFTERIMAGE_DEBUG_AABB);
        SHOW_CAUSAL_PURSUIT_DEBUG_AABB =
                builder.comment("Render red causal pursuit AABB debug boxes.")
                        .define(
                                "showCausalPursuitDebugAabb",
                                DEFAULT_SHOW_CAUSAL_PURSUIT_DEBUG_AABB);
        SHOW_CAUSAL_DEBUG_HUD =
                builder.comment("Render causal link diagnostic HUD text.")
                        .define("showCausalDebugHud", DEFAULT_SHOW_CAUSAL_DEBUG_HUD);
        builder.pop();
        CLIENT_SPEC = builder.build();
    }

    private TimeEngineClientConfig() {}

    public static boolean showGhostDebugAabb() {
        return SHOW_GHOST_DEBUG_AABB.get();
    }

    public static boolean showAfterimageDebugAabb() {
        return SHOW_AFTERIMAGE_DEBUG_AABB.get();
    }

    public static boolean showCausalPursuitDebugAabb() {
        return SHOW_CAUSAL_PURSUIT_DEBUG_AABB.get();
    }

    public static boolean showCausalDebugHud() {
        return SHOW_CAUSAL_DEBUG_HUD.get();
    }
}
