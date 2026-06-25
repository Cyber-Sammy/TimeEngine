package com.time_engine.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.common.network.TemporalEntityRenderState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ClientGhostTargetingTest {
    private static final UUID IGNORED_ID = UUID.fromString("3ab37758-33fe-48f7-bb58-804c5191bf20");
    private static final UUID ALLOWED_ID = UUID.fromString("e2e59ad7-688a-48ab-997c-26b083777d4c");

    @Test
    void skipsIgnoredPhantomCombatGhostsWhenSelectingAttackTarget() {
        TemporalEntityRenderState ignoredCloserGhost =
                state(IGNORED_ID, new AABB(1.0D, -0.5D, -0.5D, 2.0D, 0.5D, 0.5D), false);
        TemporalEntityRenderState allowedFartherGhost =
                state(ALLOWED_ID, new AABB(3.0D, -0.5D, -0.5D, 4.0D, 0.5D, 0.5D), true);

        Optional<TemporalEntityRenderState> selected =
                ClientGhostTargeting.findNearestAttackableGhost(
                        List.of(ignoredCloserGhost, allowedFartherGhost),
                        Vec3.ZERO,
                        new Vec3(1.0D, 0.0D, 0.0D),
                        5.0D);

        assertEquals(ALLOWED_ID, selected.orElseThrow().entityId());
    }

    @Test
    void returnsEmptyWhenOnlyIntersectedGhostsIgnorePhantomCombat() {
        TemporalEntityRenderState ignoredGhost =
                state(IGNORED_ID, new AABB(1.0D, -0.5D, -0.5D, 2.0D, 0.5D, 0.5D), false);

        Optional<TemporalEntityRenderState> selected =
                ClientGhostTargeting.findNearestAttackableGhost(
                        List.of(ignoredGhost), Vec3.ZERO, new Vec3(1.0D, 0.0D, 0.0D), 5.0D);

        assertTrue(selected.isEmpty());
    }

    private static TemporalEntityRenderState state(
            UUID entityId, AABB boundingBox, boolean phantomCombatAllowed) {
        return new TemporalEntityRenderState(
                entityId,
                boundingBox.getCenter(),
                0.0F,
                0.0F,
                Pose.STANDING,
                boundingBox,
                phantomCombatAllowed);
    }
}
