package com.time_engine.engine.common.temporal;

import java.util.UUID;

public final class TemporalSession {
    private final UUID sessionId;
    private final UUID ownerPlayerId;
    private final int startTick;
    private final int durationTicks;
    private final int cooldownTicks;
    private final float timeScale;
    private final double radius;
    private boolean active;

    public TemporalSession(
            UUID sessionId,
            UUID ownerPlayerId,
            int startTick,
            int durationTicks,
            int cooldownTicks,
            float timeScale,
            double radius) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("cooldownTicks must not be negative");
        }
        if (timeScale <= 0.0F) {
            throw new IllegalArgumentException("timeScale must be in the range (0, 1]");
        }
        if (timeScale > 1.0F) {
            throw new IllegalArgumentException("timeScale must be in the range (0, 1]");
        }
        if (radius <= 0.0D) {
            throw new IllegalArgumentException("radius must be positive");
        }

        this.sessionId = sessionId;
        this.ownerPlayerId = ownerPlayerId;
        this.startTick = startTick;
        this.durationTicks = durationTicks;
        this.cooldownTicks = cooldownTicks;
        this.timeScale = timeScale;
        this.radius = radius;
        this.active = true;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID ownerPlayerId() {
        return ownerPlayerId;
    }

    public int startTick() {
        return startTick;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public float timeScale() {
        return timeScale;
    }

    public double radius() {
        return radius;
    }

    public boolean active() {
        return active;
    }

    public int endTick() {
        return startTick + durationTicks;
    }

    public boolean isExpired(int currentServerTick) {
        return currentServerTick >= endTick();
    }

    public int minimumSnapshotHistoryTicks(int safetyMarginTicks) {
        int maximumPerceptionDelay = (int) Math.ceil(durationTicks * (1.0D - timeScale));
        return maximumPerceptionDelay + safetyMarginTicks;
    }

    void deactivate() {
        active = false;
    }
}
