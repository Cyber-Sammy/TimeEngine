package com.time_engine.engine.common.network;

import com.time_engine.TimeEngine;
import com.time_engine.engine.common.temporal.TemporalSession;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TemporalStatePayload(
        boolean active,
        UUID sessionId,
        int serverTick,
        int startTick,
        int durationTicks,
        float timeScale,
        double radius,
        double phantomAttackReach,
        int cooldownEndTick)
        implements CustomPacketPayload {
    private static final UUID NO_SESSION = new UUID(0L, 0L);

    public static final Type<TemporalStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TimeEngine.MOD_ID, "temporal_state"));
    public static final StreamCodec<FriendlyByteBuf, TemporalStatePayload> STREAM_CODEC =
            StreamCodec.of(TemporalStatePayload::encode, TemporalStatePayload::decode);

    public static TemporalStatePayload active(
            TemporalSession session,
            int serverTick,
            int cooldownEndTick,
            double phantomAttackReach) {
        return new TemporalStatePayload(
                true,
                session.sessionId(),
                serverTick,
                session.startTick(),
                session.durationTicks(),
                session.timeScale(),
                session.radius(),
                phantomAttackReach,
                cooldownEndTick);
    }

    public static TemporalStatePayload inactive(
            int serverTick, int cooldownEndTick, double phantomAttackReach) {
        return new TemporalStatePayload(
                false,
                NO_SESSION,
                serverTick,
                0,
                0,
                1.0F,
                0.0D,
                phantomAttackReach,
                cooldownEndTick);
    }

    private static void encode(FriendlyByteBuf buffer, TemporalStatePayload payload) {
        buffer.writeBoolean(payload.active);
        buffer.writeUUID(payload.sessionId);
        buffer.writeVarInt(payload.serverTick);
        buffer.writeVarInt(payload.startTick);
        buffer.writeVarInt(payload.durationTicks);
        buffer.writeFloat(payload.timeScale);
        buffer.writeDouble(payload.radius);
        buffer.writeDouble(payload.phantomAttackReach);
        buffer.writeVarInt(payload.cooldownEndTick);
    }

    private static TemporalStatePayload decode(FriendlyByteBuf buffer) {
        return new TemporalStatePayload(
                buffer.readBoolean(),
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
