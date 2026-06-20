package com.time_engine.common.network;

import com.time_engine.TimeEngine;
import com.time_engine.config.TemporalConfigSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TemporalConfigPayload(
        TemporalConfigSnapshot current,
        TemporalConfigSnapshot defaults,
        boolean success,
        String message)
        implements CustomPacketPayload {
    private static final int MAX_MESSAGE_LENGTH = 256;

    public static final Type<TemporalConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TimeEngine.MOD_ID, "temporal_config"));
    public static final StreamCodec<FriendlyByteBuf, TemporalConfigPayload> STREAM_CODEC =
            StreamCodec.of(TemporalConfigPayload::encode, TemporalConfigPayload::decode);

    private static void encode(FriendlyByteBuf buffer, TemporalConfigPayload payload) {
        payload.current.encode(buffer);
        payload.defaults.encode(buffer);
        buffer.writeBoolean(payload.success);
        buffer.writeUtf(payload.message, MAX_MESSAGE_LENGTH);
    }

    private static TemporalConfigPayload decode(FriendlyByteBuf buffer) {
        return new TemporalConfigPayload(
                TemporalConfigSnapshot.decode(buffer),
                TemporalConfigSnapshot.decode(buffer),
                buffer.readBoolean(),
                buffer.readUtf(MAX_MESSAGE_LENGTH));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
