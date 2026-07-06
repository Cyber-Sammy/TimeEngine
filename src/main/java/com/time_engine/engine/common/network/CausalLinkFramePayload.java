package com.time_engine.engine.common.network;

import com.time_engine.TimeEngine;
import com.time_engine.engine.common.causal.CausalLinkState;
import com.time_engine.engine.util.ModLog;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CausalLinkFramePayload(
        UUID ownerId,
        UUID targetId,
        CausalLinkState state,
        int serverTick,
        double progress,
        ResourceLocation dimension,
        TemporalEntityRenderState pursuitFrame,
        TemporalEntityRenderState targetFrame,
        TemporalEntityRenderState originOwnerFrame,
        TemporalEntityRenderState originTargetFrame)
        implements CustomPacketPayload {
    private static final int MAX_STATE_NAME_LENGTH = 64;
    private static final AtomicBoolean INVALID_STATE_LOGGED = new AtomicBoolean();

    public static final Type<CausalLinkFramePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(TimeEngine.MOD_ID, "causal_link_frame"));
    public static final StreamCodec<FriendlyByteBuf, CausalLinkFramePayload> STREAM_CODEC =
            StreamCodec.of(CausalLinkFramePayload::encode, CausalLinkFramePayload::decode);

    public CausalLinkFramePayload {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(pursuitFrame, "pursuitFrame");
        Objects.requireNonNull(targetFrame, "targetFrame");
        Objects.requireNonNull(originOwnerFrame, "originOwnerFrame");
        Objects.requireNonNull(originTargetFrame, "originTargetFrame");
        if (!Double.isFinite(progress)) {
            throw new IllegalArgumentException("progress must be finite");
        }
        progress = Math.max(0.0D, Math.min(1.0D, progress));
    }

    private static void encode(FriendlyByteBuf buffer, CausalLinkFramePayload payload) {
        buffer.writeUUID(payload.ownerId);
        buffer.writeUUID(payload.targetId);
        buffer.writeUtf(payload.state.name(), MAX_STATE_NAME_LENGTH);
        buffer.writeVarInt(payload.serverTick);
        buffer.writeDouble(payload.progress);
        buffer.writeResourceLocation(payload.dimension);
        TemporalEntityRenderState.STREAM_CODEC.encode(buffer, payload.pursuitFrame);
        TemporalEntityRenderState.STREAM_CODEC.encode(buffer, payload.targetFrame);
        TemporalEntityRenderState.STREAM_CODEC.encode(buffer, payload.originOwnerFrame);
        TemporalEntityRenderState.STREAM_CODEC.encode(buffer, payload.originTargetFrame);
    }

    private static CausalLinkFramePayload decode(FriendlyByteBuf buffer) {
        return new CausalLinkFramePayload(
                buffer.readUUID(),
                buffer.readUUID(),
                decodeState(buffer.readUtf(MAX_STATE_NAME_LENGTH)),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readResourceLocation(),
                TemporalEntityRenderState.STREAM_CODEC.decode(buffer),
                TemporalEntityRenderState.STREAM_CODEC.decode(buffer),
                TemporalEntityRenderState.STREAM_CODEC.decode(buffer),
                TemporalEntityRenderState.STREAM_CODEC.decode(buffer));
    }

    private static CausalLinkState decodeState(String stateName) {
        try {
            return CausalLinkState.valueOf(stateName);
        } catch (IllegalArgumentException exception) {
            logInvalidStateOnce(stateName);
            return CausalLinkState.BROKEN;
        }
    }

    private static void logInvalidStateOnce(String stateName) {
        if (!INVALID_STATE_LOGGED.compareAndSet(false, true)) {
            return;
        }
        ModLog.warn(
                "Received unknown causal link state '{}'; falling back to BROKEN. Further invalid states will not be logged",
                stateName);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
