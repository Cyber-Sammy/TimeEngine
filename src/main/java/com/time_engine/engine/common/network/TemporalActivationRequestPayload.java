package com.time_engine.engine.common.network;

import com.time_engine.TimeEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TemporalActivationRequestPayload() implements CustomPacketPayload {
    public static final TemporalActivationRequestPayload INSTANCE =
            new TemporalActivationRequestPayload();
    public static final Type<TemporalActivationRequestPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            TimeEngine.MOD_ID, "temporal_activation_request"));
    public static final StreamCodec<FriendlyByteBuf, TemporalActivationRequestPayload>
            STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
