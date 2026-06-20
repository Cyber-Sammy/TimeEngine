package com.time_engine.common.network;

import com.time_engine.TimeEngine;
import com.time_engine.common.snapshot.EntitySnapshot;
import com.time_engine.util.ModLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record GhostFramePayload(
        UUID sessionId,
        int serverTick,
        double perceivedTick,
        ResourceLocation dimension,
        List<GhostEntityState> entities)
        implements CustomPacketPayload {
    private static final int MAX_ENTITY_STATES = 2048;
    private static final int MAX_POSE_NAME_LENGTH = 64;
    private static final AtomicBoolean INVALID_POSE_LOGGED = new AtomicBoolean();

    static final StreamCodec<FriendlyByteBuf, Pose> POSE_CODEC =
            StreamCodec.of(
                    (buffer, pose) -> buffer.writeUtf(pose.name(), MAX_POSE_NAME_LENGTH),
                    GhostFramePayload::decodePose);

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
            List<EntitySnapshot> snapshots) {
        return new GhostFramePayload(
                sessionId,
                serverTick,
                perceivedTick,
                dimension,
                snapshots.stream().map(GhostEntityState::fromSnapshot).toList());
    }

    private static void encode(FriendlyByteBuf buffer, GhostFramePayload payload) {
        buffer.writeUUID(payload.sessionId);
        buffer.writeVarInt(payload.serverTick);
        buffer.writeDouble(payload.perceivedTick);
        buffer.writeResourceLocation(payload.dimension);
        buffer.writeVarInt(payload.entities.size());
        payload.entities.forEach(entity -> entity.encode(buffer));
    }

    private static GhostFramePayload decode(FriendlyByteBuf buffer) {
        UUID sessionId = buffer.readUUID();
        int serverTick = buffer.readVarInt();
        double perceivedTick = buffer.readDouble();
        ResourceLocation dimension = buffer.readResourceLocation();
        int entityCount = buffer.readVarInt();
        if (entityCount < 0) {
            throw new IllegalArgumentException("Invalid ghost entity state count: " + entityCount);
        }
        if (entityCount > MAX_ENTITY_STATES) {
            throw new IllegalArgumentException("Invalid ghost entity state count: " + entityCount);
        }

        List<GhostEntityState> entities = new ArrayList<>(entityCount);
        for (int index = 0; index < entityCount; index++) {
            entities.add(GhostEntityState.decode(buffer));
        }
        return new GhostFramePayload(sessionId, serverTick, perceivedTick, dimension, entities);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record GhostEntityState(
            UUID entityId, Vec3 position, float yRot, float xRot, Pose pose, AABB boundingBox) {
        public GhostEntityState {
            Objects.requireNonNull(entityId, "entityId");
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(pose, "pose");
            Objects.requireNonNull(boundingBox, "boundingBox");
        }

        public static GhostEntityState fromSnapshot(EntitySnapshot snapshot) {
            return new GhostEntityState(
                    snapshot.entityId(),
                    snapshot.position(),
                    snapshot.yRot(),
                    snapshot.xRot(),
                    snapshot.pose(),
                    snapshot.boundingBox());
        }

        public GhostEntityState interpolate(GhostEntityState next, double progress) {
            if (!entityId.equals(next.entityId)) {
                throw new IllegalArgumentException("Cannot interpolate different ghost entities");
            }
            double amount = Mth.clamp(progress, 0.0D, 1.0D);
            return new GhostEntityState(
                    entityId,
                    position.lerp(next.position, amount),
                    lerpRotation(yRot, next.yRot, amount),
                    lerpRotation(xRot, next.xRot, amount),
                    amount < 1.0D ? pose : next.pose,
                    interpolate(boundingBox, next.boundingBox, amount));
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(entityId);
            buffer.writeDouble(position.x);
            buffer.writeDouble(position.y);
            buffer.writeDouble(position.z);
            buffer.writeFloat(yRot);
            buffer.writeFloat(xRot);
            POSE_CODEC.encode(buffer, pose);
            buffer.writeDouble(boundingBox.minX);
            buffer.writeDouble(boundingBox.minY);
            buffer.writeDouble(boundingBox.minZ);
            buffer.writeDouble(boundingBox.maxX);
            buffer.writeDouble(boundingBox.maxY);
            buffer.writeDouble(boundingBox.maxZ);
        }

        private static GhostEntityState decode(FriendlyByteBuf buffer) {
            UUID entityId = buffer.readUUID();
            Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            float yRot = buffer.readFloat();
            float xRot = buffer.readFloat();
            Pose pose = POSE_CODEC.decode(buffer);
            AABB boundingBox =
                    new AABB(
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble());
            return new GhostEntityState(entityId, position, yRot, xRot, pose, boundingBox);
        }

        private static float lerpRotation(float from, float to, double progress) {
            return from + (float) (Mth.wrapDegrees(to - from) * progress);
        }

        private static AABB interpolate(AABB from, AABB to, double progress) {
            return new AABB(
                    Mth.lerp(progress, from.minX, to.minX),
                    Mth.lerp(progress, from.minY, to.minY),
                    Mth.lerp(progress, from.minZ, to.minZ),
                    Mth.lerp(progress, from.maxX, to.maxX),
                    Mth.lerp(progress, from.maxY, to.maxY),
                    Mth.lerp(progress, from.maxZ, to.maxZ));
        }
    }

    private static Pose decodePose(FriendlyByteBuf buffer) {
        String poseName = buffer.readUtf(MAX_POSE_NAME_LENGTH);
        try {
            return Pose.valueOf(poseName);
        } catch (IllegalArgumentException exception) {
            if (INVALID_POSE_LOGGED.compareAndSet(false, true)) {
                ModLog.warn(
                        "Received unknown ghost pose '{}'; falling back to STANDING. Further invalid poses will not be logged",
                        poseName);
            }
            return Pose.STANDING;
        }
    }
}
