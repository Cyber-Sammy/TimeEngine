package com.time_engine.common.combat;

import java.util.OptionalDouble;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PhantomHitDetector {
    private static final double MIN_DIRECTION_LENGTH_SQUARED = 1.0E-12D;

    private PhantomHitDetector() {}

    public static OptionalDouble rayIntersectionDistance(
            Vec3 origin, Vec3 direction, double reach, AABB targetBounds) {
        if (reach <= 0.0D) {
            return OptionalDouble.empty();
        }
        if (direction.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
            return OptionalDouble.empty();
        }
        if (targetBounds.contains(origin)) {
            return OptionalDouble.of(0.0D);
        }

        Vec3 end = origin.add(direction.normalize().scale(reach));
        return targetBounds
                .clip(origin, end)
                .map(hit -> OptionalDouble.of(origin.distanceTo(hit)))
                .orElseGet(OptionalDouble::empty);
    }
}
