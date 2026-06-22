package com.time_engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TemporalConfigSnapshotTest {
    @Test
    void defaultsAreValid() {
        assertTrue(TemporalConfigSnapshot.defaults().validate().isEmpty());
    }

    @Test
    void rejectsInvalidValueWithoutInspectingRuntimeConfig() {
        TemporalConfigSnapshot defaults = TemporalConfigSnapshot.defaults();
        TemporalConfigSnapshot invalid =
                new TemporalConfigSnapshot(
                        defaults.diagnosticLogging(),
                        0,
                        defaults.cooldownTicks(),
                        defaults.timeScale(),
                        defaults.radius(),
                        defaults.snapshotHistoryTicks(),
                        defaults.maxTrackedEntities(),
                        defaults.snapshotPlayersAlways(),
                        defaults.ghostFrameIntervalTicks(),
                        defaults.afterimageIntervalTicks(),
                        defaults.afterimageLifetimeTicks(),
                        defaults.afterimageObserverRadius(),
                        defaults.temporalInterceptEnabled(),
                        defaults.maxTemporalBlocksPerSession(),
                        defaults.maxInterceptCorrectionDistance(),
                        defaults.phantomAttackReach(),
                        defaults.phantomDamageMultiplier(),
                        defaults.phantomAttackCooldownTicks(),
                        defaults.phantomAllowedHitTickDrift());

        assertEquals("durationTicks must be between 1 and 12000", invalid.validate().orElseThrow());
    }

    @Test
    void codecRoundTripsAllValues() {
        TemporalConfigSnapshot expected = TemporalConfigSnapshot.defaults();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            expected.encode(buffer);

            assertEquals(expected, TemporalConfigSnapshot.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsInvalidAfterimageLifetime() {
        TemporalConfigSnapshot defaults = TemporalConfigSnapshot.defaults();
        TemporalConfigSnapshot invalid =
                new TemporalConfigSnapshot(
                        defaults.diagnosticLogging(),
                        defaults.durationTicks(),
                        defaults.cooldownTicks(),
                        defaults.timeScale(),
                        defaults.radius(),
                        defaults.snapshotHistoryTicks(),
                        defaults.maxTrackedEntities(),
                        defaults.snapshotPlayersAlways(),
                        defaults.ghostFrameIntervalTicks(),
                        defaults.afterimageIntervalTicks(),
                        0,
                        defaults.afterimageObserverRadius(),
                        defaults.temporalInterceptEnabled(),
                        defaults.maxTemporalBlocksPerSession(),
                        defaults.maxInterceptCorrectionDistance(),
                        defaults.phantomAttackReach(),
                        defaults.phantomDamageMultiplier(),
                        defaults.phantomAttackCooldownTicks(),
                        defaults.phantomAllowedHitTickDrift());

        assertEquals(
                "afterimageLifetimeTicks must be between 1 and 200",
                invalid.validate().orElseThrow());
    }

    @Test
    void rejectsInvalidInterceptCorrectionDistance() {
        TemporalConfigSnapshot defaults = TemporalConfigSnapshot.defaults();
        TemporalConfigSnapshot invalid =
                new TemporalConfigSnapshot(
                        defaults.diagnosticLogging(),
                        defaults.durationTicks(),
                        defaults.cooldownTicks(),
                        defaults.timeScale(),
                        defaults.radius(),
                        defaults.snapshotHistoryTicks(),
                        defaults.maxTrackedEntities(),
                        defaults.snapshotPlayersAlways(),
                        defaults.ghostFrameIntervalTicks(),
                        defaults.afterimageIntervalTicks(),
                        defaults.afterimageLifetimeTicks(),
                        defaults.afterimageObserverRadius(),
                        defaults.temporalInterceptEnabled(),
                        defaults.maxTemporalBlocksPerSession(),
                        100.0D,
                        defaults.phantomAttackReach(),
                        defaults.phantomDamageMultiplier(),
                        defaults.phantomAttackCooldownTicks(),
                        defaults.phantomAllowedHitTickDrift());

        assertEquals(
                "maxInterceptCorrectionDistance must be between 1.0 and 64.0",
                invalid.validate().orElseThrow());
    }
}
