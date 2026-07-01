package com.time_engine.engine.common.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemporalLayerRelationTest {
    @Test
    void equalScalesAreSameLayer() {
        TemporalLayerRelation relation = TemporalLayerRelation.compare(0.2D, 0.2D);

        assertEquals(TemporalLayerRelation.Kind.SAME_LAYER, relation.kind());
        assertFalse(relation.allowsAttackableGhost());
    }

    @Test
    void observerWithLowerScaleIsFaster() {
        TemporalLayerRelation relation = TemporalLayerRelation.compare(0.2D, 0.5D);

        assertEquals(TemporalLayerRelation.Kind.OBSERVER_FASTER, relation.kind());
        assertTrue(relation.allowsAttackableGhost());
    }

    @Test
    void targetWithLowerScaleIsFaster() {
        TemporalLayerRelation relation = TemporalLayerRelation.compare(0.5D, 0.2D);

        assertEquals(TemporalLayerRelation.Kind.TARGET_FASTER, relation.kind());
        assertFalse(relation.allowsAttackableGhost());
    }

    @Test
    void normalTargetIsSlowerThanTemporalObserver() {
        TemporalLayerRelation relation = TemporalLayerRelation.compare(0.5D, 1.0D);

        assertEquals(TemporalLayerRelation.Kind.OBSERVER_FASTER, relation.kind());
        assertTrue(relation.allowsAttackableGhost());
    }

    @Test
    void epsilonPreventsNearEqualScalesFromCreatingGhostAdvantage() {
        TemporalLayerRelation relation = TemporalLayerRelation.compare(0.2D, 0.20005D);

        assertEquals(TemporalLayerRelation.Kind.SAME_LAYER, relation.kind());
        assertFalse(relation.allowsAttackableGhost());
    }

    @Test
    void relativePerceivedTickUsesScaleDifference() {
        TemporalSession session =
                new TemporalSession(
                        UUID.randomUUID(), UUID.randomUUID(), 100, 200, 20, 0.2F, 16.0D);
        TemporalLayerRelation relation = TemporalLayerRelation.compare(0.2D, 0.5D);

        assertEquals(170.0D, relation.relativePerceivedTick(session, 200), 0.0001D);
    }

    @Test
    void relativePerceivedTickForNormalTargetMatchesExistingSlowdown() {
        TemporalSession session =
                new TemporalSession(
                        UUID.randomUUID(), UUID.randomUUID(), 100, 200, 20, 0.2F, 16.0D);
        TemporalLayerRelation relation = TemporalLayerRelation.compare(0.2D, 1.0D);

        assertEquals(120.0D, relation.relativePerceivedTick(session, 200), 0.0001D);
    }
}
