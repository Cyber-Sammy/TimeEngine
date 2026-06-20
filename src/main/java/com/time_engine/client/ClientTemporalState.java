package com.time_engine.client;

import com.time_engine.common.network.TemporalStatePayload;
import java.util.UUID;
import net.minecraft.client.Minecraft;

public final class ClientTemporalState {
    private static boolean active;
    private static UUID sessionId;
    private static int serverTickAtSync;
    private static long clientTickAtSync;
    private static int startTick;
    private static int durationTicks;
    private static float timeScale = 1.0F;
    private static double radius;
    private static double phantomAttackReach;
    private static int cooldownEndTick;

    private ClientTemporalState() {}

    public static void apply(TemporalStatePayload payload) {
        active = payload.active();
        sessionId = payload.active() ? payload.sessionId() : null;
        serverTickAtSync = payload.serverTick();
        clientTickAtSync = currentClientTick();
        startTick = payload.startTick();
        durationTicks = payload.durationTicks();
        timeScale = payload.timeScale();
        radius = payload.radius();
        phantomAttackReach = payload.phantomAttackReach();
        cooldownEndTick = payload.cooldownEndTick();
    }

    public static boolean isActive() {
        return active && estimatedServerTick() < startTick + durationTicks;
    }

    public static int activeTicksRemaining() {
        return isActive() ? Math.max(0, startTick + durationTicks - estimatedServerTick()) : 0;
    }

    public static int cooldownTicksRemaining() {
        return Math.max(0, cooldownEndTick - estimatedServerTick());
    }

    public static float timeScale() {
        return timeScale;
    }

    public static double radius() {
        return radius;
    }

    public static UUID sessionId() {
        return sessionId;
    }

    public static double phantomAttackReach() {
        return phantomAttackReach;
    }

    public static void reset() {
        active = false;
        sessionId = null;
        serverTickAtSync = 0;
        clientTickAtSync = 0L;
        startTick = 0;
        durationTicks = 0;
        timeScale = 1.0F;
        radius = 0.0D;
        phantomAttackReach = 0.0D;
        cooldownEndTick = 0;
    }

    private static int estimatedServerTick() {
        return serverTickAtSync + (int) Math.max(0L, currentClientTick() - clientTickAtSync);
    }

    private static long currentClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }
}
