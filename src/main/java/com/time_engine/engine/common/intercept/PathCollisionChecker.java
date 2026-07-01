package com.time_engine.engine.common.intercept;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PathCollisionChecker {
    private static final double MIN_PATH_LENGTH = 1.0E-9D;
    static final double COLLISION_TOLERANCE = 0.02D;

    private PathCollisionChecker() {}

    public static OptionalDouble findFirstCollision(
            AABB previousBounds, AABB currentBounds, List<AABB> obstacles) {
        Vec3 start = previousBounds.getCenter();
        Vec3 end = currentBounds.getCenter();
        double pathLength = start.distanceTo(end);
        double halfWidth = Math.max(previousBounds.getXsize(), currentBounds.getXsize()) * 0.5D;
        double halfHeight = Math.max(previousBounds.getYsize(), currentBounds.getYsize()) * 0.5D;
        double halfDepth = Math.max(previousBounds.getZsize(), currentBounds.getZsize()) * 0.5D;

        double earliestCollision = Double.POSITIVE_INFINITY;
        for (AABB obstacle : obstacles) {
            AABB expanded =
                    obstacle.inflate(
                            halfWidth + COLLISION_TOLERANCE,
                            halfHeight + COLLISION_TOLERANCE,
                            halfDepth + COLLISION_TOLERANCE);
            OptionalDouble collision = intersectionFraction(start, end, pathLength, expanded);
            if (collision.isEmpty()) {
                continue;
            }
            if (isNavigableTopContact(
                    previousBounds, currentBounds, obstacle, collision.getAsDouble())) {
                continue;
            }
            earliestCollision = Math.min(earliestCollision, collision.getAsDouble());
        }
        if (!Double.isFinite(earliestCollision)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(earliestCollision);
    }

    public static boolean overlaps(AABB entityBounds, List<AABB> obstacles) {
        for (AABB obstacle : obstacles) {
            if (obstacle.inflate(COLLISION_TOLERANCE).intersects(entityBounds)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNavigableTopContact(
            AABB previousBounds, AABB currentBounds, AABB obstacle, double collisionProgress) {
        double minimumClearance = obstacle.maxY - COLLISION_TOLERANCE;
        if (currentBounds.minY < minimumClearance) {
            return false;
        }
        double bottomAtCollision =
                Mth.lerp(collisionProgress, previousBounds.minY, currentBounds.minY);
        return bottomAtCollision + MIN_PATH_LENGTH >= minimumClearance;
    }

    private static OptionalDouble intersectionFraction(
            Vec3 start, Vec3 end, double pathLength, AABB expandedObstacle) {
        if (expandedObstacle.contains(start)) {
            return OptionalDouble.of(0.0D);
        }
        if (pathLength < MIN_PATH_LENGTH) {
            return OptionalDouble.empty();
        }

        Optional<Vec3> intersection = expandedObstacle.clip(start, end);
        if (intersection.isEmpty()) {
            return OptionalDouble.empty();
        }
        double fraction = start.distanceTo(intersection.orElseThrow()) / pathLength;
        return OptionalDouble.of(Mth.clamp(fraction, 0.0D, 1.0D));
    }
}
