package com.time_engine.engine.client;

import com.time_engine.engine.common.network.AfterimagePayload;
import com.time_engine.engine.common.network.TemporalEntityRenderState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Mth;

final class AfterimageTrail {
    private static final int MAX_ANCHORS = 128;
    private static final double MIN_ANCHOR_DISTANCE_SQUARED = 0.01D;

    private final Deque<StoredAnchor> anchors = new ArrayDeque<>();
    private UUID sessionId;
    private int latestServerTick = Integer.MIN_VALUE;

    void add(AfterimagePayload payload, long receivedAtTick) {
        if (!payload.sessionId().equals(sessionId)) {
            reset(payload.sessionId());
        }
        if (payload.serverTick() <= latestServerTick) {
            return;
        }

        latestServerTick = payload.serverTick();
        if (isAtLatestPosition(payload.state())) {
            return;
        }
        anchors.addLast(
                new StoredAnchor(
                        payload.state(),
                        receivedAtTick + payload.lifetimeTicks(),
                        payload.lifetimeTicks()));
        trimToCapacity();
    }

    void prune(long currentTick) {
        anchors.removeIf(anchor -> anchor.expiresAtTick() <= currentTick);
    }

    List<RenderedAfterimage> getRenderStates(long currentTick, float partialTick) {
        double renderTick = currentTick + partialTick;
        List<RenderedAfterimage> states = new ArrayList<>(anchors.size());
        for (StoredAnchor anchor : anchors) {
            float alpha = alpha(anchor, renderTick);
            if (alpha <= 0.0F) {
                continue;
            }
            states.add(new RenderedAfterimage(anchor.state(), alpha));
        }
        return states;
    }

    boolean isEmpty() {
        return anchors.isEmpty();
    }

    int size() {
        return anchors.size();
    }

    private void reset(UUID newSessionId) {
        anchors.clear();
        sessionId = newSessionId;
        latestServerTick = Integer.MIN_VALUE;
    }

    private void trimToCapacity() {
        while (anchors.size() > MAX_ANCHORS) {
            anchors.removeFirst();
        }
    }

    private boolean isAtLatestPosition(TemporalEntityRenderState state) {
        StoredAnchor latest = anchors.peekLast();
        if (latest == null) {
            return false;
        }
        return latest.state().position().distanceToSqr(state.position())
                < MIN_ANCHOR_DISTANCE_SQUARED;
    }

    private static float alpha(StoredAnchor anchor, double renderTick) {
        double remainingTicks = anchor.expiresAtTick() - renderTick;
        return (float) Mth.clamp(remainingTicks / anchor.lifetimeTicks(), 0.0D, 1.0D);
    }

    record RenderedAfterimage(TemporalEntityRenderState state, float alpha) {}

    private record StoredAnchor(
            TemporalEntityRenderState state, long expiresAtTick, int lifetimeTicks) {}
}
