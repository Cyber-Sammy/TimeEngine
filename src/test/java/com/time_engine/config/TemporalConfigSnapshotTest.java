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
                        defaults.trackNewEntitiesEnteringSessionRadius(),
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
    void sessionSettingsOverlayOnlyChangesSessionValues() {
        TemporalConfigSnapshot defaults = TemporalConfigSnapshot.defaults();
        TemporalSessionSettings settings = new TemporalSessionSettings(600, 40, 0.5F, 48.0D);

        TemporalConfigSnapshot actual = defaults.withSessionSettings(settings);

        assertEquals(600, actual.durationTicks());
        assertEquals(40, actual.cooldownTicks());
        assertEquals(0.5D, actual.timeScale(), 0.0001D);
        assertEquals(48.0D, actual.radius());
        assertEquals(defaults.snapshotHistoryTicks(), actual.snapshotHistoryTicks());
        assertEquals(
                defaults.trackNewEntitiesEnteringSessionRadius(),
                actual.trackNewEntitiesEnteringSessionRadius());
        assertEquals(defaults.phantomAttackReach(), actual.phantomAttackReach());
        assertEquals(defaults.temporalInterceptEnabled(), actual.temporalInterceptEnabled());
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
                        defaults.trackNewEntitiesEnteringSessionRadius(),
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
                        defaults.trackNewEntitiesEnteringSessionRadius(),
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
