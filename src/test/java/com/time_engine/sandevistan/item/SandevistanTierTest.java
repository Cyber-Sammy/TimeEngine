package com.time_engine.sandevistan.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SandevistanTierTest {
    @Test
    void mk1UsesIntroductorySessionOptions() {
        assertTier(SandevistanTier.MK1, 160, 240, 0.50F, 24.0D);
    }

    @Test
    void mk2UsesIntermediateSessionOptions() {
        assertTier(SandevistanTier.MK2, 220, 300, 0.35F, 32.0D);
    }

    @Test
    void mk3UsesStrongestSessionOptions() {
        assertTier(SandevistanTier.MK3, 300, 420, 0.20F, 40.0D);
    }

    private static void assertTier(
            SandevistanTier tier,
            int durationTicks,
            int cooldownTicks,
            float timeScale,
            double radius) {
        assertEquals(durationTicks, tier.options().durationTicks());
        assertEquals(cooldownTicks, tier.options().cooldownTicks());
        assertEquals(timeScale, tier.options().timeScale());
        assertEquals(radius, tier.options().radius());
    }
}
