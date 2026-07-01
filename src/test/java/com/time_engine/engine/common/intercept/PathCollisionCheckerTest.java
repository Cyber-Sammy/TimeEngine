package com.time_engine.engine.common.intercept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class PathCollisionCheckerTest {
    private static final AABB BLOCK = new AABB(2.0D, 0.0D, 0.0D, 3.0D, 1.0D, 1.0D);

    @Test
    void detectsSweptCollision() {
        AABB previous = entityBounds(0.0D, 0.0D);
        AABB current = entityBounds(4.0D, 0.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isPresent());
        assertEquals(0.245D, collision.getAsDouble(), 0.0001D);
    }

    @Test
    void rejectsPathBesideObstacle() {
        AABB previous = entityBounds(0.0D, 3.0D);
        AABB current = entityBounds(4.0D, 3.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isEmpty());
    }

    @Test
    void detectsExactEdgeContact() {
        AABB previous = entityBounds(0.0D, 1.0D);
        AABB current = entityBounds(4.0D, 1.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isPresent());
    }

    @Test
    void detectsNearEdgeContactWithinTolerance() {
        AABB previous = entityBounds(0.0D, 1.015D);
        AABB current = entityBounds(4.0D, 1.015D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isPresent());
    }

    @Test
    void rejectsPathOutsideTolerance() {
        AABB previous = entityBounds(0.0D, 1.03D);
        AABB current = entityBounds(4.0D, 1.03D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isEmpty());
    }

    @Test
    void detectsDiagonalCornerContact() {
        AABB previous = entityBounds(0.0D, 3.0D);
        AABB current = entityBounds(2.5D, 1.015D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isPresent());
    }

    @Test
    void ignoresJumpThatClearsTopSurface() {
        AABB previous = entityBounds(0.0D, 0.0D, 0.0D);
        AABB current = entityBounds(4.0D, 4.0D, 0.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isEmpty());
    }

    @Test
    void ignoresMovementAlongTopSurface() {
        AABB previous = entityBounds(2.0D, 1.0D, 0.0D);
        AABB current = entityBounds(4.0D, 1.0D, 0.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isEmpty());
    }

    @Test
    void detectsDescendingPathThatEntersBlock() {
        AABB previous = entityBounds(2.0D, 2.0D, 0.0D);
        AABB current = entityBounds(2.0D, 0.0D, 0.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertTrue(collision.isPresent());
    }

    @Test
    void detectsHeadContactWithBlockUnderside() {
        AABB overheadBlock = new AABB(2.0D, 2.0D, 0.0D, 3.0D, 3.0D, 1.0D);
        AABB previous = entityBounds(2.0D, 0.0D, 0.0D);
        AABB current = entityBounds(2.0D, 2.0D, 0.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(overheadBlock));

        assertTrue(collision.isPresent());
    }

    @Test
    void reportsZeroWhenPathStartsInsideObstacle() {
        AABB previous = entityBounds(2.0D, 0.0D);
        AABB current = entityBounds(4.0D, 0.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(previous, current, List.of(BLOCK));

        assertEquals(0.0D, collision.orElseThrow(), 0.0001D);
    }

    @Test
    void stationaryEntityOutsideObstacleDoesNotCollide() {
        AABB bounds = entityBounds(0.0D, 0.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(bounds, bounds, List.of(BLOCK));

        assertTrue(collision.isEmpty());
    }

    @Test
    void choosesEarliestObstacle() {
        AABB previous = entityBounds(0.0D, 0.0D);
        AABB current = entityBounds(8.0D, 0.0D);
        AABB laterBlock = new AABB(6.0D, 0.0D, 0.0D, 7.0D, 1.0D, 1.0D);

        OptionalDouble collision =
                PathCollisionChecker.findFirstCollision(
                        previous, current, List.of(laterBlock, BLOCK));

        assertEquals(0.1225D, collision.orElseThrow(), 0.0001D);
    }

    private static AABB entityBounds(double x, double z) {
        return entityBounds(x, 0.0D, z);
    }

    private static AABB entityBounds(double x, double y, double z) {
        return new AABB(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
    }
}
