package com.time_engine.api;

import java.util.UUID;

public record TemporalSessionState(
        UUID sessionId,
        UUID ownerPlayerId,
        boolean active,
        int startTick,
        int durationTicks,
        int cooldownTicks,
        float timeScale,
        double radius,
        int serverTick,
        double perceivedTick,
        int activeTicksRemaining,
        int cooldownTicksRemaining) {}
