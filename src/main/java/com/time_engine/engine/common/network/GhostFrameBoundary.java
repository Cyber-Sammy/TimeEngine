package com.time_engine.engine.common.network;

import com.time_engine.engine.common.snapshot.EntitySnapshot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GhostFrameBoundary {
    private GhostFrameBoundary() {}

    public static EntitySnapshot clampToRadius(
            EntitySnapshot snapshot, Vec3 center, double radius) {
        Vec3 offset = snapshot.position().subtract(center);
        double distanceSquared = offset.lengthSqr();
        double radiusSquared = radius * radius;
        if (distanceSquared <= radiusSquared) {
            return snapshot;
        }
        if (distanceSquared == 0.0D) {
            return snapshot;
        }

        Vec3 clampedPosition = center.add(offset.normalize().scale(radius));
        Vec3 movement = clampedPosition.subtract(snapshot.position());
        return new EntitySnapshot(
                snapshot.entityId(),
                snapshot.serverTick(),
                snapshot.dimension(),
                clampedPosition,
                snapshot.velocity(),
                snapshot.yRot(),
                snapshot.xRot(),
                snapshot.pose(),
                snapshot.boundingBox().move(movement),
                snapshot.alive(),
                snapshot.health());
    }

    public static boolean isWithinRadius(EntitySnapshot snapshot, Vec3 center, double radius) {
        return snapshot.position().distanceToSqr(center) <= radius * radius;
    }

    public static boolean canRenderAtBoundary(EntitySnapshot snapshot, Vec3 center, double radius) {
        if (isWithinRadius(snapshot, center, radius)) {
            return true;
        }
        double distance = snapshot.position().distanceTo(center);
        return distance <= radius + maximumHalfExtent(snapshot.boundingBox());
    }

    private static double maximumHalfExtent(AABB boundingBox) {
        double halfX = boundingBox.getXsize() * 0.5D;
        double halfY = boundingBox.getYsize() * 0.5D;
        double halfZ = boundingBox.getZsize() * 0.5D;
        return Math.max(halfX, Math.max(halfY, halfZ));
    }
}
