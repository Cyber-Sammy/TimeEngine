package com.time_engine.engine.common.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemporalScaleResolverTest {
    @Test
    void normalTargetUsesFullObserverSlowdown() {
        TemporalSession observer = session(100, 0.2F);

        double perceivedTick =
                TemporalScaleResolver.relativePerceivedTick(observer, List.of(), 200);

        assertEquals(120.0D, perceivedTick, 0.0001D);
    }

    @Test
    void targetStartingLaterOnlyUsesRelativeSlowdownDuringOverlap() {
        TemporalSession observer = session(100, 0.2F);
        TemporalScaleSegment targetSession = new TemporalScaleSegment(180, 300, 0.5D);

        double perceivedTick =
                TemporalScaleResolver.relativePerceivedTick(observer, List.of(targetSession), 200);

        assertEquals(130.0D, perceivedTick, 0.0001D);
    }

    @Test
    void targetEndingEarlierReturnsToNormalSlowdownAfterOverlap() {
        TemporalSession observer = session(100, 0.2F);
        TemporalScaleSegment targetSession = new TemporalScaleSegment(150, 200, 0.5D);

        double perceivedTick =
                TemporalScaleResolver.relativePerceivedTick(observer, List.of(targetSession), 220);

        assertEquals(149.0D, perceivedTick, 0.0001D);
    }

    @Test
    void equalScaleOverlapDoesNotAddDelayDuringOverlap() {
        TemporalSession observer = session(100, 0.2F);
        TemporalScaleSegment targetSession = new TemporalScaleSegment(100, 200, 0.2D);

        double perceivedTick =
                TemporalScaleResolver.relativePerceivedTick(observer, List.of(targetSession), 200);

        assertEquals(200.0D, perceivedTick, 0.0001D);
    }

    private static TemporalSession session(int startTick, float scale) {
        return new TemporalSession(
                UUID.randomUUID(), UUID.randomUUID(), startTick, 400, 20, scale, 32.0D);
    }
}
