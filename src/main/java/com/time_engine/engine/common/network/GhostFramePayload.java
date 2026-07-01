package com.time_engine.engine.common.network;

import com.time_engine.TimeEngine;
import com.time_engine.engine.common.snapshot.EntitySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GhostFramePayload(
        UUID sessionId,
        int serverTick,
        double perceivedTick,
        ResourceLocation dimension,
        List<TemporalEntityRenderState> entities)
        implements CustomPacketPayload {
    private static final int MAX_ENTITY_STATES = 2048;

    public static final Type<GhostFramePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TimeEngine.MOD_ID, "ghost_frame"));
    public static final StreamCodec<FriendlyByteBuf, GhostFramePayload> STREAM_CODEC =
            StreamCodec.of(GhostFramePayload::encode, GhostFramePayload::decode);

    public GhostFramePayload {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(dimension, "dimension");
        entities = List.copyOf(entities);
        if (entities.size() > MAX_ENTITY_STATES) {
            throw new IllegalArgumentException("Too many ghost entity states");
        }
    }

    public static GhostFramePayload create(
            UUID sessionId,
            int serverTick,
            double perceivedTick,
            ResourceLocation dimension,
            List<GhostFrameEntity> ghostEntities) {
        return new GhostFramePayload(
                sessionId,
                serverTick,
                perceivedTick,
                dimension,
                ghostEntities.stream().map(GhostFrameEntity::toRenderState).toList());
    }

    private static void encode(FriendlyByteBuf buffer, GhostFramePayload payload) {
        buffer.writeUUID(payload.sessionId);
        buffer.writeVarInt(payload.serverTick);
        buffer.writeDouble(payload.perceivedTick);
        buffer.writeResourceLocation(payload.dimension);
        buffer.writeVarInt(payload.entities.size());
        payload.entities.forEach(
                entity -> TemporalEntityRenderState.STREAM_CODEC.encode(buffer, entity));
    }

    private static GhostFramePayload decode(FriendlyByteBuf buffer) {
        UUID sessionId = buffer.readUUID();
        int serverTick = buffer.readVarInt();
        double perceivedTick = buffer.readDouble();
        ResourceLocation dimension = buffer.readResourceLocation();
        int entityCount = buffer.readVarInt();
        validateEntityCount(entityCount);

        List<TemporalEntityRenderState> entities = new ArrayList<>(entityCount);
        for (int index = 0; index < entityCount; index++) {
            entities.add(TemporalEntityRenderState.STREAM_CODEC.decode(buffer));
        }
        return new GhostFramePayload(sessionId, serverTick, perceivedTick, dimension, entities);
    }

    private static void validateEntityCount(int entityCount) {
        if (entityCount < 0) {
            throw new IllegalArgumentException("Invalid ghost entity state count: " + entityCount);
        }
        if (entityCount > MAX_ENTITY_STATES) {
            throw new IllegalArgumentException("Invalid ghost entity state count: " + entityCount);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record GhostFrameEntity(
            EntitySnapshot snapshot, double perceivedTick, boolean phantomCombatAllowed) {
        public GhostFrameEntity {
            Objects.requireNonNull(snapshot, "snapshot");
            if (!Double.isFinite(perceivedTick)) {
                throw new IllegalArgumentException("perceivedTick must be finite");
            }
        }

        private TemporalEntityRenderState toRenderState() {
            return TemporalEntityRenderState.fromSnapshot(
                    snapshot, perceivedTick, phantomCombatAllowed);
        }
    }
}
