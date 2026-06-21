package com.time_engine.common.network;

import com.time_engine.TimeEngine;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AfterimagePayload(
        UUID sessionId,
        int serverTick,
        ResourceLocation dimension,
        int lifetimeTicks,
        TemporalEntityRenderState state)
        implements CustomPacketPayload {
    public static final Type<AfterimagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TimeEngine.MOD_ID, "afterimage"));
    public static final StreamCodec<FriendlyByteBuf, AfterimagePayload> STREAM_CODEC =
            StreamCodec.of(AfterimagePayload::encode, AfterimagePayload::decode);

    public AfterimagePayload {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(state, "state");
        if (lifetimeTicks <= 0) {
            throw new IllegalArgumentException("lifetimeTicks must be positive");
        }
    }

    private static void encode(FriendlyByteBuf buffer, AfterimagePayload payload) {
        buffer.writeUUID(payload.sessionId);
        buffer.writeVarInt(payload.serverTick);
        buffer.writeResourceLocation(payload.dimension);
        buffer.writeVarInt(payload.lifetimeTicks);
        TemporalEntityRenderState.STREAM_CODEC.encode(buffer, payload.state);
    }

    private static AfterimagePayload decode(FriendlyByteBuf buffer) {
        return new AfterimagePayload(
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                TemporalEntityRenderState.STREAM_CODEC.decode(buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
