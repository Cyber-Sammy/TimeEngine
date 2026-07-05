package com.time_engine.engine.common.causal;

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
}
