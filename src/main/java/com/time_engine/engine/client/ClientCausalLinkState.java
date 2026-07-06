package com.time_engine.engine.client;

import com.time_engine.engine.common.network.CausalLinkFramePayload;
import com.time_engine.engine.common.network.TemporalEntityRenderState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClientCausalLinkState {
    private static final int STALE_FRAME_TICKS = 20;
    private static final Map<UUID, ReceivedFrame> FRAMES_BY_OWNER = new HashMap<>();
    private static ResourceLocation currentDimension;

    private ClientCausalLinkState() {}

    public static void apply(CausalLinkFramePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canApply(minecraft, payload)) {
            return;
        }
        synchronizeDimension(minecraft);
        FRAMES_BY_OWNER.put(
                payload.ownerId(), new ReceivedFrame(payload, minecraft.level.getGameTime()));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasActiveWorld(minecraft)) {
            clear();
            return;
        }
        synchronizeDimension(minecraft);
        long currentTick = minecraft.level.getGameTime();
        FRAMES_BY_OWNER
                .values()
                .removeIf(frame -> currentTick - frame.receivedAtTick() > STALE_FRAME_TICKS);
    }

    public static List<TemporalEntityRenderState> getRenderStates() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasActiveWorld(minecraft)) {
            return List.of();
        }
        UUID localPlayerId = minecraft.player.getUUID();
        long currentTick = minecraft.level.getGameTime();
        return FRAMES_BY_OWNER.values().stream()
                .filter(frame -> currentTick - frame.receivedAtTick() <= STALE_FRAME_TICKS)
                .filter(frame -> frame.payload().targetId().equals(localPlayerId))
                .map(frame -> frame.payload().pursuitFrame())
                .toList();
    }

    public static List<CausalLinkDebugView> getDebugViews() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasActiveWorld(minecraft)) {
            return List.of();
        }
        long currentTick = minecraft.level.getGameTime();
        return FRAMES_BY_OWNER.values().stream()
                .map(
                        frame ->
                                CausalLinkDebugView.from(
                                        frame.payload(), currentTick - frame.receivedAtTick()))
                .toList();
    }

    public static void clear() {
        FRAMES_BY_OWNER.clear();
        currentDimension = null;
    }

    private static boolean canApply(Minecraft minecraft, CausalLinkFramePayload payload) {
        if (!hasActiveWorld(minecraft)) {
            return false;
        }
        if (!isLocalParticipant(minecraft.player.getUUID(), payload)) {
            return false;
        }
        return minecraft.level.dimension().location().equals(payload.dimension());
    }

    private static boolean isLocalParticipant(UUID localPlayerId, CausalLinkFramePayload payload) {
        if (localPlayerId.equals(payload.ownerId())) {
            return true;
        }
        return localPlayerId.equals(payload.targetId());
    }

    private static boolean hasActiveWorld(Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.level != null;
    }

    private static void synchronizeDimension(Minecraft minecraft) {
        ResourceLocation dimension = minecraft.level.dimension().location();
        if (dimension.equals(currentDimension)) {
            return;
        }
        FRAMES_BY_OWNER.clear();
        currentDimension = dimension;
    }

    private record ReceivedFrame(CausalLinkFramePayload payload, long receivedAtTick) {}

    public record CausalLinkDebugView(
            UUID ownerId,
            UUID targetId,
            String state,
            int serverTick,
            double progress,
            int staleTicks,
            double pursuitDistanceToTarget,
            double targetDistanceFromOrigin) {
        private static CausalLinkDebugView from(CausalLinkFramePayload payload, long staleTicks) {
            return new CausalLinkDebugView(
                    payload.ownerId(),
                    payload.targetId(),
                    payload.state().name(),
                    payload.serverTick(),
                    payload.progress(),
                    Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(0L, staleTicks))),
                    payload.pursuitFrame().position().distanceTo(payload.targetFrame().position()),
                    payload.targetFrame()
                            .position()
                            .distanceTo(payload.originTargetFrame().position()));
        }

        public boolean stale() {
            return staleTicks > STALE_FRAME_TICKS / 2;
        }

        public String shortOwnerId() {
            return shortId(ownerId);
        }

        public String shortTargetId() {
            return shortId(targetId);
        }

        private static String shortId(UUID id) {
            return id.toString().substring(0, 8);
        }
    }
}
