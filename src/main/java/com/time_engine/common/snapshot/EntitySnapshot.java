package com.time_engine.common.snapshot;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record EntitySnapshot(
        UUID entityId,
        int serverTick,
        ResourceKey<Level> dimension,
        Vec3 position,
        Vec3 velocity,
        float yRot,
        float xRot,
        Pose pose,
        AABB boundingBox,
        boolean alive,
        float health) {
    public static final float UNKNOWN_HEALTH = Float.NaN;

    public EntitySnapshot {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(velocity, "velocity");
        Objects.requireNonNull(pose, "pose");
        Objects.requireNonNull(boundingBox, "boundingBox");
    }

    public static EntitySnapshot capture(Entity entity, int serverTick) {
        float health =
                entity instanceof LivingEntity livingEntity
                        ? livingEntity.getHealth()
                        : UNKNOWN_HEALTH;

        return new EntitySnapshot(
                entity.getUUID(),
                serverTick,
                entity.level().dimension(),
                entity.position(),
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                entity.getPose(),
                entity.getBoundingBox(),
                entity.isAlive(),
                health);
    }

    public boolean hasHealth() {
        return Float.isFinite(health);
    }

    public EntitySnapshot interpolate(EntitySnapshot next, double progress) {
        Objects.requireNonNull(next, "next");
        if (!entityId.equals(next.entityId)) {
            throw new IllegalArgumentException(
                    "Cannot interpolate snapshots from different entities");
        }

        double clampedProgress = Mth.clamp(progress, 0.0D, 1.0D);
        if (!dimension.equals(next.dimension)) {
            return clampedProgress < 0.5D ? this : next;
        }

        return new EntitySnapshot(
                entityId,
                serverTick,
                dimension,
                position.lerp(next.position, clampedProgress),
                velocity.lerp(next.velocity, clampedProgress),
                lerpRotation(yRot, next.yRot, clampedProgress),
                lerpRotation(xRot, next.xRot, clampedProgress),
                clampedProgress < 1.0D ? pose : next.pose,
                interpolate(boundingBox, next.boundingBox, clampedProgress),
                clampedProgress < 1.0D ? alive : next.alive,
                interpolateHealth(next, clampedProgress));
    }

    private float interpolateHealth(EntitySnapshot next, double progress) {
        if (!hasHealth()) {
            return progress < 1.0D ? health : next.health;
        }
        if (!next.hasHealth()) {
            return progress < 1.0D ? health : next.health;
        }
        return (float) Mth.lerp(progress, health, next.health);
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
