package com.time_engine.engine.common.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PhantomHitDetectorTest {
    private static final Vec3 ORIGIN = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Vec3 FORWARD = new Vec3(0.0D, 0.0D, 1.0D);

    @Test
    void detectsIntersectionWithinReach() {
        AABB target = new AABB(-0.5D, 0.0D, 3.0D, 0.5D, 2.0D, 4.0D);

        OptionalDouble distance =
                PhantomHitDetector.rayIntersectionDistance(ORIGIN, FORWARD, 5.0D, target);

        assertTrue(distance.isPresent());
        assertEquals(3.0D, distance.getAsDouble(), 0.0001D);
    }

    @Test
    void rejectsBoxOutsideReach() {
        AABB target = new AABB(-0.5D, 0.0D, 6.0D, 0.5D, 2.0D, 7.0D);

        assertTrue(
                PhantomHitDetector.rayIntersectionDistance(ORIGIN, FORWARD, 5.0D, target)
                        .isEmpty());
    }

    @Test
    void rejectsRayThatMissesBox() {
        AABB target = new AABB(2.0D, 0.0D, 3.0D, 3.0D, 2.0D, 4.0D);

        assertTrue(
                PhantomHitDetector.rayIntersectionDistance(ORIGIN, FORWARD, 5.0D, target)
                        .isEmpty());
    }

    @Test
    void acceptsOriginInsideBox() {
        AABB target = new AABB(-1.0D, 0.0D, -1.0D, 1.0D, 2.0D, 1.0D);

        OptionalDouble distance =
                PhantomHitDetector.rayIntersectionDistance(ORIGIN, FORWARD, 5.0D, target);

        assertTrue(distance.isPresent());
        assertEquals(0.0D, distance.getAsDouble(), 0.0001D);
    }
}
