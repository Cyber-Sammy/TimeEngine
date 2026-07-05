package com.time_engine.engine.common.causal;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CausalTargetCandidate(
        UUID targetId,
        Vec3 ghostPosition,
        AABB authoritativeBounds,
        boolean selfTarget,
        boolean temporalAdvantageAllowed) {
    public CausalTargetCandidate {
        if (targetId == null) {
            throw new IllegalArgumentException("targetId must not be null");
        }
        if (ghostPosition == null) {
            throw new IllegalArgumentException("ghostPosition must not be null");
        }
        if (authoritativeBounds == null) {
            throw new IllegalArgumentException("authoritativeBounds must not be null");
        }
    }

    public static CausalTargetCandidate target(
            UUID targetId, Vec3 ghostPosition, AABB authoritativeBounds) {
        return new CausalTargetCandidate(targetId, ghostPosition, authoritativeBounds, false, true);
    }

    public double distanceTo(Vec3 userPosition) {
        return ghostPosition.distanceTo(userPosition);
    }

    public Vec3 directionFrom(Vec3 userPosition) {
        Vec3 direction = ghostPosition.subtract(userPosition);
        if (direction.lengthSqr() <= 1.0E-8D) {
            return Vec3.ZERO;
        }
        return direction.normalize();
    }
}
