package com.time_engine.engine.common.causal;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CausalPhantomFrame(
        UUID entityId,
        int serverTick,
        Vec3 stableAnchor,
        AABB authoritativeBounds,
        CausalPhantomPoseState pose,
        CausalPhantomActionState action,
        CausalPhantomEquipmentState equipment) {
    public CausalPhantomFrame {
        if (entityId == null) {
            throw new IllegalArgumentException("entityId must not be null");
        }
        if (stableAnchor == null) {
            throw new IllegalArgumentException("stableAnchor must not be null");
        }
        if (authoritativeBounds == null) {
            throw new IllegalArgumentException("authoritativeBounds must not be null");
        }
        if (pose == null) {
            throw new IllegalArgumentException("pose must not be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (equipment == null) {
            throw new IllegalArgumentException("equipment must not be null");
        }
    }
}
