package com.time_engine.common.network;

import com.time_engine.TimeEngine;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhantomHitRequestPayload(UUID targetEntityId, double clientPerceivedTick)
        implements CustomPacketPayload {
    public static final Type<PhantomHitRequestPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            TimeEngine.MOD_ID, "phantom_hit_request"));
    public static final StreamCodec<FriendlyByteBuf, PhantomHitRequestPayload> STREAM_CODEC =
            StreamCodec.of(PhantomHitRequestPayload::encode, PhantomHitRequestPayload::decode);

    private static void encode(FriendlyByteBuf buffer, PhantomHitRequestPayload payload) {
        buffer.writeUUID(payload.targetEntityId);
        buffer.writeDouble(payload.clientPerceivedTick);
    }

    private static PhantomHitRequestPayload decode(FriendlyByteBuf buffer) {
        return new PhantomHitRequestPayload(buffer.readUUID(), buffer.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
