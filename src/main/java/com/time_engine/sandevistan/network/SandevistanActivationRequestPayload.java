package com.time_engine.sandevistan.network;

import com.time_engine.TimeEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SandevistanActivationRequestPayload() implements CustomPacketPayload {
    public static final SandevistanActivationRequestPayload INSTANCE =
            new SandevistanActivationRequestPayload();
    public static final Type<SandevistanActivationRequestPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            TimeEngine.MOD_ID, "sandevistan_activation_request"));
    public static final StreamCodec<FriendlyByteBuf, SandevistanActivationRequestPayload>
            STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
