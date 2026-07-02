package com.time_engine.engine.common.network;

import com.time_engine.engine.common.snapshot.EntitySnapshot;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GhostFrameBoundary {
    private static final double MIN_SEGMENT_LENGTH = 1.0E-9D;

    private GhostFrameBoundary() {}

    public static Optional<EntitySnapshot> resolveSegment(
            Optional<EntitySnapshot> previousSnapshot,
            EntitySnapshot currentSnapshot,
            Vec3 center,
            double radius) {
        if (isWithinRadius(currentSnapshot, center, radius)) {
            return Optional.of(currentSnapshot);
        }
        if (previousSnapshot.isEmpty()) {
            return resolveCurrentOnly(currentSnapshot, center, radius);
        }

        EntitySnapshot previous = previousSnapshot.orElseThrow();
        if (!isContinuousSegment(previous, currentSnapshot)) {
            return resolveCurrentOnly(currentSnapshot, center, radius);
        }
        if (isWithinRadius(previous, center, radius)) {
            return boundaryCrossing(previous, currentSnapshot, center, radius);
        }
        return Optional.empty();
    }

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

    private static Optional<EntitySnapshot> resolveCurrentOnly(
            EntitySnapshot currentSnapshot, Vec3 center, double radius) {
        if (!canRenderAtBoundary(currentSnapshot, center, radius)) {
            return Optional.empty();
        }
        return Optional.of(clampToRadius(currentSnapshot, center, radius));
    }

    private static Optional<EntitySnapshot> boundaryCrossing(
            EntitySnapshot previous, EntitySnapshot current, Vec3 center, double radius) {
        Vec3 start = previous.position().subtract(center);
        Vec3 end = current.position().subtract(center);
        Vec3 movement = end.subtract(start);
        double a = movement.lengthSqr();
        if (a < MIN_SEGMENT_LENGTH) {
            return Optional.of(clampToRadius(current, center, radius));
        }

        double b = 2.0D * start.dot(movement);
        double c = start.lengthSqr() - radius * radius;
        double discriminant = b * b - 4.0D * a * c;
        if (discriminant < 0.0D) {
            return Optional.of(clampToRadius(current, center, radius));
        }

        double root = Math.sqrt(discriminant);
        double first = (-b - root) / (2.0D * a);
        double second = (-b + root) / (2.0D * a);
        double progress = selectExitProgress(first, second);
        EntitySnapshot crossing = previous.interpolate(current, progress);
        return Optional.of(clampToRadius(crossing, center, radius));
    }

    private static double selectExitProgress(double first, double second) {
        if (first >= 0.0D && first <= 1.0D) {
            return Mth.clamp(first, 0.0D, 1.0D);
        }
        return Mth.clamp(second, 0.0D, 1.0D);
    }

    private static boolean isContinuousSegment(EntitySnapshot previous, EntitySnapshot current) {
        if (!previous.entityId().equals(current.entityId())) {
            return false;
        }
        return previous.dimension().equals(current.dimension());
    }
}
