package com.time_engine.engine.common.causal;

import com.time_engine.engine.common.snapshot.EntitySnapshot;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
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

    public static CausalPhantomFrame capture(Entity entity, int serverTick) {
        return new CausalPhantomFrame(
                entity.getUUID(),
                serverTick,
                entity.position(),
                entity.getBoundingBox(),
                CausalPhantomPoseState.capture(entity),
                CausalPhantomActionState.capture(entity),
                CausalPhantomEquipmentState.capture(entity));
    }

    public static CausalPhantomFrame fromSnapshot(EntitySnapshot snapshot) {
        return new CausalPhantomFrame(
                snapshot.entityId(),
                snapshot.serverTick(),
                snapshot.position(),
                snapshot.boundingBox(),
                CausalPhantomPoseState.fromSnapshot(snapshot),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }
}
