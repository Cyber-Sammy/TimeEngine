package com.time_engine.api.client;

import java.util.UUID;

public record TemporalClientStateView(
        boolean active,
        UUID sessionId,
        int activeTicksRemaining,
        int cooldownTicksRemaining,
        float timeScale,
        double radius,
        double phantomAttackReach) {}
