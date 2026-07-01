package com.time_engine.engine.common.intercept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.engine.common.snapshot.EntitySnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class SafeSnapshotResolverTest {
    private static final AABB BLOCK = new AABB(2.0D, 0.0D, 0.0D, 3.0D, 1.0D, 1.0D);

    @Test
    void resolvesPositionImmediatelyBeforeImpact() {
        EntitySnapshot previous = snapshot(10, 0.0D);
        EntitySnapshot current = snapshot(11, 4.0D);
        double collisionProgress =
                PathCollisionChecker.findFirstCollision(
                                previous.boundingBox(), current.boundingBox(), List.of(BLOCK))
                        .orElseThrow();

        EntitySnapshot safe =
                SafeSnapshotResolver.resolve(
                                previous,
                                current,
                                collisionProgress,
                                10.0D,
                                0.0D,
                                List.of(BLOCK),
                                ignored -> Optional.empty())
                        .orElseThrow();

        assertTrue(safe.position().x < 1.0D);
        assertFalse(PathCollisionChecker.overlaps(safe.boundingBox(), List.of(BLOCK)));
    }

    @Test
    void searchesBackwardWhenSegmentStartsOverlapped() {
        EntitySnapshot previous = snapshot(10, 2.0D);
        EntitySnapshot current = snapshot(11, 3.0D);
        EntitySnapshot earlierSafe = snapshot(9, 0.0D);

        Optional<EntitySnapshot> resolved =
                SafeSnapshotResolver.resolve(
                        previous,
                        current,
                        0.0D,
                        10.0D,
                        0.0D,
                        List.of(BLOCK),
                        tick -> tick == 9.0D ? Optional.of(earlierSafe) : Optional.empty());

        assertEquals(earlierSafe, resolved.orElseThrow());
    }

    @Test
    void rejectsStartOverlapWithoutSafeHistory() {
        EntitySnapshot previous = snapshot(10, 2.0D);
        EntitySnapshot current = snapshot(11, 3.0D);

        Optional<EntitySnapshot> resolved =
                SafeSnapshotResolver.resolve(
                        previous,
                        current,
                        0.0D,
                        10.0D,
                        0.0D,
                        List.of(BLOCK),
                        ignored -> Optional.empty());

        assertTrue(resolved.isEmpty());
    }

    @Test
    void rejectsSafeHistoryThatIsPastObstacleAlongMovementDirection() {
        EntitySnapshot previous = snapshot(10, 2.0D);
        EntitySnapshot current = snapshot(11, 3.0D);
        EntitySnapshot safeButPastObstacle = snapshot(9, 4.0D);

        Optional<EntitySnapshot> resolved =
                SafeSnapshotResolver.resolve(
                        previous,
                        current,
                        0.0D,
                        10.0D,
                        0.0D,
                        List.of(BLOCK),
                        tick -> tick == 9.0D ? Optional.of(safeButPastObstacle) : Optional.empty());

        assertTrue(resolved.isEmpty());
    }

    private static EntitySnapshot snapshot(int serverTick, double x) {
        return new EntitySnapshot(
                UUID.fromString("f430bed8-9440-46af-bbf2-c5633a73f49d"),
                serverTick,
                Level.OVERWORLD,
                new Vec3(x, 0.0D, 0.0D),
                Vec3.ZERO,
                0.0F,
                0.0F,
                Pose.STANDING,
                new AABB(x, 0.0D, 0.0D, x + 1.0D, 1.0D, 1.0D),
                true,
                20.0F);
    }
}
