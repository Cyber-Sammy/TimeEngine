package com.time_engine.client;

import com.time_engine.common.network.GhostFramePayload;
import com.time_engine.common.network.GhostFramePayload.GhostEntityState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class ClientGhostState {
    private static final int STALE_FRAME_TICKS = 20;

    private static GhostFramePayload previousFrame;
    private static GhostFramePayload currentFrame;
    private static long currentFrameReceivedAtTick;

    private ClientGhostState() {}

    public static void apply(GhostFramePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !ClientTemporalState.isActive()
                || !Objects.equals(ClientTemporalState.sessionId(), payload.sessionId())
                || !minecraft.level.dimension().location().equals(payload.dimension())) {
            clear();
            return;
        }
        if (currentFrame != null && payload.serverTick() <= currentFrame.serverTick()) {
            return;
        }

        previousFrame = currentFrame;
        currentFrame = payload;
        currentFrameReceivedAtTick = minecraft.level.getGameTime();
    }

    public static List<GhostEntityState> getRenderStates(float partialTick) {
        return getRenderedFrame(partialTick).map(RenderedGhostFrame::entities).orElseGet(List::of);
    }

    public static Optional<RenderedGhostFrame> getRenderedFrame(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || currentFrame == null
                || minecraft.level.getGameTime() - currentFrameReceivedAtTick > STALE_FRAME_TICKS) {
            return Optional.empty();
        }
        if (previousFrame == null || !previousFrame.sessionId().equals(currentFrame.sessionId())) {
            return Optional.of(
                    new RenderedGhostFrame(currentFrame.perceivedTick(), currentFrame.entities()));
        }

        int frameInterval = Math.max(1, currentFrame.serverTick() - previousFrame.serverTick());
        double elapsed = minecraft.level.getGameTime() - currentFrameReceivedAtTick + partialTick;
        double progress = Mth.clamp(elapsed / frameInterval, 0.0D, 1.0D);
        Map<UUID, GhostEntityState> previousById = new HashMap<>();
        previousFrame.entities().forEach(state -> previousById.put(state.entityId(), state));
        List<GhostEntityState> entities =
                currentFrame.entities().stream()
                        .map(
                                current -> {
                                    GhostEntityState previous =
                                            previousById.get(current.entityId());
                                    return previous == null
                                            ? current
                                            : previous.interpolate(current, progress);
                                })
                        .toList();
        double perceivedTick =
                Mth.lerp(progress, previousFrame.perceivedTick(), currentFrame.perceivedTick());
        return Optional.of(new RenderedGhostFrame(perceivedTick, entities));
    }

    public static void clear() {
        previousFrame = null;
        currentFrame = null;
        currentFrameReceivedAtTick = 0L;
    }

    public record RenderedGhostFrame(double perceivedTick, List<GhostEntityState> entities) {}
}
