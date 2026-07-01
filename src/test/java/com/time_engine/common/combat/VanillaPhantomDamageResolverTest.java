package com.time_engine.common.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VanillaPhantomDamageResolverTest {
    @Test
    void calculatesDamageFromBaseAttackDamageAndMultiplier() {
        assertEquals(6.0F, VanillaPhantomDamageResolver.calculateDamage(3.0D, 2.0F), 0.0001F);
    }

    @Test
    void keepsZeroDamageWhenBaseAttackDamageIsZero() {
        assertEquals(0.0F, VanillaPhantomDamageResolver.calculateDamage(0.0D, 2.0F), 0.0001F);
    }

    @Test
    void keepsNegativeMultiplierResultVisibleForCallerRejection() {
        assertEquals(-3.0F, VanillaPhantomDamageResolver.calculateDamage(3.0D, -1.0F), 0.0001F);
    }
}
