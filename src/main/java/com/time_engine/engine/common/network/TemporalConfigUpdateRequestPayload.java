package com.time_engine.engine.common.network;

import com.time_engine.TimeEngine;
import com.time_engine.engine.config.TemporalConfigSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TemporalConfigUpdateRequestPayload(TemporalConfigSnapshot requested)
        implements CustomPacketPayload {
    public static final Type<TemporalConfigUpdateRequestPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            TimeEngine.MOD_ID, "temporal_config_update"));
    public static final StreamCodec<FriendlyByteBuf, TemporalConfigUpdateRequestPayload>
            STREAM_CODEC =
                    StreamCodec.of(
                            TemporalConfigUpdateRequestPayload::encode,
                            TemporalConfigUpdateRequestPayload::decode);

    private static void encode(FriendlyByteBuf buffer, TemporalConfigUpdateRequestPayload payload) {
        payload.requested.encode(buffer);
    }

    private static TemporalConfigUpdateRequestPayload decode(FriendlyByteBuf buffer) {
        return new TemporalConfigUpdateRequestPayload(TemporalConfigSnapshot.decode(buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
