package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalProgressCalculatorTest {
    @Test
    void progressIsZeroAtStart() {
        assertEquals(
                0.0D,
                CausalProgressCalculator.progress(
                        Vec3.ZERO, Vec3.ZERO, new Vec3(10.0D, 0.0D, 0.0D)));
    }

    @Test
    void progressIsOneAtTarget() {
        Vec3 target = new Vec3(10.0D, 0.0D, 0.0D);

        assertEquals(1.0D, CausalProgressCalculator.progress(Vec3.ZERO, target, target));
    }

    @Test
    void progressIsClamped() {
        assertEquals(
                0.0D,
                CausalProgressCalculator.progress(
                        Vec3.ZERO, new Vec3(-5.0D, 0.0D, 0.0D), new Vec3(10.0D, 0.0D, 0.0D)));
    }
}
