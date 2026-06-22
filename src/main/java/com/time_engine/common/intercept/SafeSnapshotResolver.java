package com.time_engine.common.intercept;

import com.time_engine.common.snapshot.EntitySnapshot;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;
import net.minecraft.world.phys.AABB;

final class SafeSnapshotResolver {
    private static final int MAX_HISTORY_SEARCH_TICKS = 20;
    private static final double SAFE_DISTANCE_BEFORE_COLLISION = 0.05D;
    private static final double MIN_PATH_LENGTH = 1.0E-9D;

    private SafeSnapshotResolver() {}

    static Optional<EntitySnapshot> resolve(
            EntitySnapshot previousSnapshot,
            EntitySnapshot currentSnapshot,
            double collisionProgress,
            double previousPerceivedTick,
            double minimumPerceivedTick,
            List<AABB> obstacles,
            DoubleFunction<Optional<EntitySnapshot>> historyLookup) {
        Optional<EntitySnapshot> segmentSnapshot =
                resolveWithinSegment(
                        previousSnapshot, currentSnapshot, collisionProgress, obstacles);
        if (segmentSnapshot.isPresent()) {
            return segmentSnapshot;
        }
        return searchHistory(
                previousSnapshot,
                previousPerceivedTick,
                minimumPerceivedTick,
                obstacles,
                historyLookup);
    }

    private static Optional<EntitySnapshot> resolveWithinSegment(
            EntitySnapshot previousSnapshot,
            EntitySnapshot currentSnapshot,
            double collisionProgress,
            List<AABB> obstacles) {
        if (collisionProgress <= 0.0D) {
            return Optional.empty();
        }

        double pathLength = previousSnapshot.position().distanceTo(currentSnapshot.position());
        if (pathLength < MIN_PATH_LENGTH) {
            return Optional.empty();
        }
        double progressMargin = SAFE_DISTANCE_BEFORE_COLLISION / pathLength;
        double safeProgress = Math.max(0.0D, collisionProgress - progressMargin);
        EntitySnapshot candidate = previousSnapshot.interpolate(currentSnapshot, safeProgress);
        if (PathCollisionChecker.overlaps(candidate.boundingBox(), obstacles)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    private static Optional<EntitySnapshot> searchHistory(
            EntitySnapshot previousSnapshot,
            double previousPerceivedTick,
            double minimumPerceivedTick,
            List<AABB> obstacles,
            DoubleFunction<Optional<EntitySnapshot>> historyLookup) {
        if (isSafe(previousSnapshot, obstacles)) {
            return Optional.of(previousSnapshot);
        }

        for (int offset = 1; offset <= MAX_HISTORY_SEARCH_TICKS; offset++) {
            double lookupTick = previousPerceivedTick - offset;
            if (lookupTick < minimumPerceivedTick) {
                return Optional.empty();
            }
            Optional<EntitySnapshot> snapshot = historyLookup.apply(lookupTick);
            if (snapshot.isEmpty()) {
                continue;
            }
            if (isSafe(snapshot.orElseThrow(), obstacles)) {
                return snapshot;
            }
        }
        return Optional.empty();
    }

    private static boolean isSafe(EntitySnapshot snapshot, List<AABB> obstacles) {
        if (!snapshot.alive()) {
            return false;
        }
        return !PathCollisionChecker.overlaps(snapshot.boundingBox(), obstacles);
    }
}
