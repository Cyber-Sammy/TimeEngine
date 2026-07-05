package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkDiagnosticsTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Test
    void summaryIncludesCoreLinkState() {
        CausalLink link = CausalLink.softLocked(OWNER_ID, TARGET_ID, 10, frame(TARGET_ID));
        CausalLinkView view = CausalLinkView.from(link, 15);

        String summary = CausalLinkDiagnostics.summary(view);

        assertTrue(summary.contains("owner=" + OWNER_ID));
        assertTrue(summary.contains("target=" + TARGET_ID));
        assertTrue(summary.contains("state=SOFT_LOCK"));
        assertTrue(summary.contains("age=5"));
    }

    @Test
    void detailIncludesPolicyRelationAndFrameState() {
        CausalLink link =
                CausalLink.softLocked(
                        OWNER_ID,
                        TARGET_ID,
                        10,
                        frame(TARGET_ID, new Vec3(4.0D, 0.0D, 0.0D)),
                        frame(OWNER_ID, new Vec3(1.0D, 0.0D, 0.0D)));

        String detail =
                CausalLinkDiagnostics.detail(link, 15, "Owner", "Target", true, true, true, false);

        assertTrue(detail.contains("Owner -> Target"));
        assertTrue(detail.contains("targetExists=true"));
        assertTrue(detail.contains("targetAlive=true"));
        assertTrue(detail.contains("relationAllowed=true"));
        assertTrue(detail.contains("policyAllowed=false"));
        assertTrue(detail.contains("targetFrame"));
        assertTrue(detail.contains("originTargetFrame"));
        assertTrue(detail.contains("originOwnerFrame"));
    }

    private static CausalPhantomFrame frame(UUID entityId) {
        return frame(entityId, Vec3.ZERO);
    }

    private static CausalPhantomFrame frame(UUID entityId, Vec3 anchor) {
        return new CausalPhantomFrame(
                entityId,
                10,
                anchor,
                new AABB(-0.5D, 0.0D, -0.5D, 0.5D, 1.8D, 0.5D),
                CausalPhantomPoseState.standing(),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }
}
