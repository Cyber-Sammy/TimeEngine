package com.time_engine.common.network;

import com.time_engine.TimeEngine;
import com.time_engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.common.policy.TemporalPolicyResolver;
import com.time_engine.common.snapshot.EntitySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

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
            List<EntitySnapshot> snapshots,
            EntityResolver entityResolver) {
        return new GhostFramePayload(
                sessionId,
                serverTick,
                perceivedTick,
                dimension,
                snapshots.stream()
                        .map(snapshot -> createRenderState(snapshot, entityResolver))
                        .toList());
    }

    private static TemporalEntityRenderState createRenderState(
            EntitySnapshot snapshot, EntityResolver entityResolver) {
        return TemporalEntityRenderState.fromSnapshot(
                snapshot, isPhantomCombatAllowed(snapshot, entityResolver));
    }

    private static boolean isPhantomCombatAllowed(
            EntitySnapshot snapshot, EntityResolver entityResolver) {
        Entity entity = entityResolver.resolve(snapshot.entityId());
        if (entity == null) {
            return false;
        }
        return TemporalPolicyResolver.getInstance()
                        .resolveEntity(
                                entity,
                                Operation.PHANTOM_COMBAT,
                                TemporalPolicyDefaults.phantomCombat())
                        .decision()
                == Decision.ALLOW;
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

    @FunctionalInterface
    public interface EntityResolver {
        Entity resolve(UUID entityId);
    }
}
