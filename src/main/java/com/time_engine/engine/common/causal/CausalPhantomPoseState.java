package com.time_engine.engine.common.causal;

import com.time_engine.engine.common.snapshot.EntitySnapshot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public record CausalPhantomPoseState(
        Pose pose, float yRot, float xRot, Vec3 lookDirection, Vec3 movementIntent) {
    public CausalPhantomPoseState {
        if (pose == null) {
            throw new IllegalArgumentException("pose must not be null");
        }
        if (lookDirection == null) {
            throw new IllegalArgumentException("lookDirection must not be null");
        }
        if (movementIntent == null) {
            throw new IllegalArgumentException("movementIntent must not be null");
        }
    }

    public static CausalPhantomPoseState standing() {
        return new CausalPhantomPoseState(Pose.STANDING, 0.0F, 0.0F, Vec3.ZERO, Vec3.ZERO);
    }

    public static CausalPhantomPoseState capture(Entity entity) {
        return new CausalPhantomPoseState(
                entity.getPose(),
                entity.getYRot(),
                entity.getXRot(),
                normalized(entity.getLookAngle()),
                movementIntent(entity.getDeltaMovement()));
    }

    public static CausalPhantomPoseState fromSnapshot(EntitySnapshot snapshot) {
        return new CausalPhantomPoseState(
                snapshot.pose(),
                snapshot.yRot(),
                snapshot.xRot(),
                lookDirection(snapshot.xRot(), snapshot.yRot()),
                movementIntent(snapshot.velocity()));
    }

    private static Vec3 movementIntent(Vec3 velocity) {
        return normalized(new Vec3(velocity.x, 0.0D, velocity.z));
    }

    private static Vec3 normalized(Vec3 vector) {
        if (vector.lengthSqr() <= 1.0E-8D) {
            return Vec3.ZERO;
        }
        return vector.normalize();
    }

    private static Vec3 lookDirection(float xRot, float yRot) {
        float xRadians = xRot * Mth.DEG_TO_RAD;
        float invertedYRadians = -yRot * Mth.DEG_TO_RAD;
        float yCos = Mth.cos(invertedYRadians);
        float ySin = Mth.sin(invertedYRadians);
        float xCos = Mth.cos(xRadians);
        float xSin = Mth.sin(xRadians);
        return new Vec3(ySin * xCos, -xSin, yCos * xCos).normalize();
    }
}
