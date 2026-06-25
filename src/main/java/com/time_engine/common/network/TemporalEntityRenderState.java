package com.time_engine.common.network;

import com.time_engine.common.snapshot.EntitySnapshot;
import com.time_engine.util.ModLog;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record TemporalEntityRenderState(
        UUID entityId,
        Vec3 position,
        float yRot,
        float xRot,
        Pose pose,
        AABB boundingBox,
        boolean phantomCombatAllowed) {
    private static final int MAX_POSE_NAME_LENGTH = 64;
    private static final AtomicBoolean INVALID_POSE_LOGGED = new AtomicBoolean();

    public static final StreamCodec<FriendlyByteBuf, TemporalEntityRenderState> STREAM_CODEC =
            StreamCodec.of(TemporalEntityRenderState::encode, TemporalEntityRenderState::decode);

    public TemporalEntityRenderState {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(pose, "pose");
        Objects.requireNonNull(boundingBox, "boundingBox");
    }

    public static TemporalEntityRenderState fromSnapshot(EntitySnapshot snapshot) {
        return fromSnapshot(snapshot, true);
    }

    public static TemporalEntityRenderState fromSnapshot(
            EntitySnapshot snapshot, boolean phantomCombatAllowed) {
        return new TemporalEntityRenderState(
                snapshot.entityId(),
                snapshot.position(),
                snapshot.yRot(),
                snapshot.xRot(),
                snapshot.pose(),
                snapshot.boundingBox(),
                phantomCombatAllowed);
    }

    public static TemporalEntityRenderState fromEntity(Entity entity) {
        return new TemporalEntityRenderState(
                entity.getUUID(),
                entity.position(),
                entity.getYRot(),
                entity.getXRot(),
                entity.getPose(),
                entity.getBoundingBox(),
                true);
    }

    public TemporalEntityRenderState interpolate(TemporalEntityRenderState next, double progress) {
        if (!entityId.equals(next.entityId)) {
            throw new IllegalArgumentException("Cannot interpolate different temporal entities");
        }

        double amount = Mth.clamp(progress, 0.0D, 1.0D);
        return new TemporalEntityRenderState(
                entityId,
                position.lerp(next.position, amount),
                lerpRotation(yRot, next.yRot, amount),
                lerpRotation(xRot, next.xRot, amount),
                amount < 1.0D ? pose : next.pose,
                interpolate(boundingBox, next.boundingBox, amount),
                phantomCombatAllowed && next.phantomCombatAllowed);
    }

    private static void encode(FriendlyByteBuf buffer, TemporalEntityRenderState state) {
        buffer.writeUUID(state.entityId);
        buffer.writeDouble(state.position.x);
        buffer.writeDouble(state.position.y);
        buffer.writeDouble(state.position.z);
        buffer.writeFloat(state.yRot);
        buffer.writeFloat(state.xRot);
        buffer.writeUtf(state.pose.name(), MAX_POSE_NAME_LENGTH);
        buffer.writeDouble(state.boundingBox.minX);
        buffer.writeDouble(state.boundingBox.minY);
        buffer.writeDouble(state.boundingBox.minZ);
        buffer.writeDouble(state.boundingBox.maxX);
        buffer.writeDouble(state.boundingBox.maxY);
        buffer.writeDouble(state.boundingBox.maxZ);
        buffer.writeBoolean(state.phantomCombatAllowed);
    }

    private static TemporalEntityRenderState decode(FriendlyByteBuf buffer) {
        UUID entityId = buffer.readUUID();
        Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        float yRot = buffer.readFloat();
        float xRot = buffer.readFloat();
        Pose pose = decodePose(buffer.readUtf(MAX_POSE_NAME_LENGTH));
        AABB boundingBox =
                new AABB(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble());
        boolean phantomCombatAllowed = buffer.readBoolean();
        return new TemporalEntityRenderState(
                entityId, position, yRot, xRot, pose, boundingBox, phantomCombatAllowed);
    }

    private static Pose decodePose(String poseName) {
        try {
            return Pose.valueOf(poseName);
        } catch (IllegalArgumentException exception) {
            logInvalidPoseOnce(poseName);
            return Pose.STANDING;
        }
    }

    private static void logInvalidPoseOnce(String poseName) {
        if (!INVALID_POSE_LOGGED.compareAndSet(false, true)) {
            return;
        }
        ModLog.warn(
                "Received unknown temporal pose '{}'; falling back to STANDING. Further invalid poses will not be logged",
                poseName);
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
