package com.time_engine.engine.common.causal;

import net.minecraft.world.phys.Vec3;

public final class CausalProgressCalculator {
    private CausalProgressCalculator() {}

    public static double progress(Vec3 start, Vec3 current, Vec3 target) {
        double totalDistance = start.distanceTo(target);
        if (totalDistance <= 1.0E-8D) {
            return 1.0D;
        }

        double remainingDistance = current.distanceTo(target);
        double progress = 1.0D - remainingDistance / totalDistance;
        return Math.max(0.0D, Math.min(1.0D, progress));
    }
}
