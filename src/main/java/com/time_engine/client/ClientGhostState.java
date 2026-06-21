package com.time_engine.client;

import com.time_engine.common.network.GhostFramePayload;
import com.time_engine.common.network.TemporalEntityRenderState;
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
        if (!canApply(minecraft, payload)) {
            clear();
            return;
        }
        if (isOutdated(payload)) {
            return;
        }

        previousFrame = currentFrame;
        currentFrame = payload;
        currentFrameReceivedAtTick = minecraft.level.getGameTime();
    }

    public static List<TemporalEntityRenderState> getRenderStates(float partialTick) {
        return getRenderedFrame(partialTick).map(RenderedGhostFrame::entities).orElseGet(List::of);
    }

    public static Optional<RenderedGhostFrame> getRenderedFrame(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasRenderableFrame(minecraft)) {
            return Optional.empty();
        }
        if (!canInterpolateFrames()) {
            return Optional.of(
                    new RenderedGhostFrame(currentFrame.perceivedTick(), currentFrame.entities()));
        }

        int frameInterval = Math.max(1, currentFrame.serverTick() - previousFrame.serverTick());
        double elapsed = minecraft.level.getGameTime() - currentFrameReceivedAtTick + partialTick;
        double progress = Mth.clamp(elapsed / frameInterval, 0.0D, 1.0D);
        Map<UUID, TemporalEntityRenderState> previousById = new HashMap<>();
        previousFrame.entities().forEach(state -> previousById.put(state.entityId(), state));
        List<TemporalEntityRenderState> entities =
                currentFrame.entities().stream()
                        .map(
                                current -> {
                                    TemporalEntityRenderState previous =
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

    private static boolean canApply(Minecraft minecraft, GhostFramePayload payload) {
        if (minecraft.level == null) {
            return false;
        }
        if (!ClientTemporalState.isActive()) {
            return false;
        }
        if (!Objects.equals(ClientTemporalState.sessionId(), payload.sessionId())) {
            return false;
        }
        return minecraft.level.dimension().location().equals(payload.dimension());
    }

    private static boolean isOutdated(GhostFramePayload payload) {
        if (currentFrame == null) {
            return false;
        }
        return payload.serverTick() <= currentFrame.serverTick();
    }

    private static boolean hasRenderableFrame(Minecraft minecraft) {
        if (minecraft.level == null) {
            return false;
        }
        if (currentFrame == null) {
            return false;
        }
        return minecraft.level.getGameTime() - currentFrameReceivedAtTick <= STALE_FRAME_TICKS;
    }

    private static boolean canInterpolateFrames() {
        if (previousFrame == null) {
            return false;
        }
        return previousFrame.sessionId().equals(currentFrame.sessionId());
    }

    public static void clear() {
        previousFrame = null;
        currentFrame = null;
        currentFrameReceivedAtTick = 0L;
    }

    public record RenderedGhostFrame(
            double perceivedTick, List<TemporalEntityRenderState> entities) {}
}
