package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkManagerTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER_TARGET_ID =
            UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void softRefreshDoesNotDowngradeActiveHardLock() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalTargetSelection selection = CausalTargetSelection.selected(candidate(), 10.0D);
        manager.updateSoftLock(OWNER_ID, selection, 0, frame(), 0.0D);
        manager.hardLock(OWNER_ID, 1, 20);

        CausalLink refreshed =
                manager.updateSoftLock(OWNER_ID, selection, 2, frame(), 0.5D).orElseThrow();

        assertEquals(CausalLinkState.HARD_LOCK, refreshed.state());
    }

    @Test
    void softRefreshCannotSwitchActiveHardLockToDifferentTarget() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.updateSoftLock(
                OWNER_ID,
                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                0,
                frame(TARGET_ID),
                0.0D);
        manager.hardLock(OWNER_ID, 1, 20);

        CausalLink refreshed =
                manager.updateSoftLock(
                                OWNER_ID,
                                CausalTargetSelection.selected(candidate(OTHER_TARGET_ID), 20.0D),
                                2,
                                frame(OTHER_TARGET_ID),
                                0.5D)
                        .orElseThrow();

        assertEquals(TARGET_ID, refreshed.targetId());
        assertEquals(CausalLinkState.HARD_LOCK, refreshed.state());
    }

    private static CausalTargetCandidate candidate() {
        return candidate(TARGET_ID);
    }

    private static CausalTargetCandidate candidate(UUID targetId) {
        return CausalTargetCandidate.target(targetId, Vec3.ZERO, bounds());
    }

    private static CausalPhantomFrame frame() {
        return frame(TARGET_ID);
    }

    private static CausalPhantomFrame frame(UUID targetId) {
        return new CausalPhantomFrame(
                targetId,
                0,
                Vec3.ZERO,
                bounds(),
                CausalPhantomPoseState.standing(),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }

    private static AABB bounds() {
        return new AABB(-0.5D, 0.0D, -0.5D, 0.5D, 1.8D, 0.5D);
    }
}
