package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkRuntimeServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_TARGET_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void brokenLinkCannotBeRetainedDuringSwitch() {
        CausalLink link = activeLink().breakLink(10);

        assertFalse(
                CausalLinkRuntimeService.canRetainPreviousDuringSwitch(
                        Optional.of(link), OTHER_TARGET_ID));
    }

    @Test
    void expiredLinkCannotBeRetainedDuringSwitch() {
        CausalLink link = activeLink().expire(10);

        assertFalse(
                CausalLinkRuntimeService.canRetainPreviousDuringSwitch(
                        Optional.of(link), OTHER_TARGET_ID));
    }

    @Test
    void activeIncompleteLinkCanBeRetainedDuringSwitch() {
        assertTrue(
                CausalLinkRuntimeService.canRetainPreviousDuringSwitch(
                        Optional.of(activeLink()), OTHER_TARGET_ID));
    }

    @Test
    void activeCompletedLinkCannotBeRetainedDuringSwitch() {
        CausalLink link =
                activeLink()
                        .refreshSoftLock(TARGET_ID, 2, frame(TARGET_ID), frame(OWNER_ID), 0.99D);

        assertFalse(
                CausalLinkRuntimeService.canRetainPreviousDuringSwitch(
                        Optional.of(link), OTHER_TARGET_ID));
    }

    @Test
    void sameTargetDoesNotNeedSwitchRetention() {
        assertFalse(
                CausalLinkRuntimeService.canRetainPreviousDuringSwitch(
                        Optional.of(activeLink()), TARGET_ID));
    }

    private static CausalLink activeLink() {
        CausalLink link =
                CausalLink.softLocked(OWNER_ID, TARGET_ID, 1, frame(TARGET_ID), frame(OWNER_ID));
        return link.refreshSoftLock(TARGET_ID, 2, frame(TARGET_ID), frame(OWNER_ID), 0.5D);
    }

    private static CausalPhantomFrame frame(UUID entityId) {
        return new CausalPhantomFrame(
                entityId,
                1,
                Vec3.ZERO,
                new AABB(-0.5D, 0.0D, -0.5D, 0.5D, 1.8D, 0.5D),
                CausalPhantomPoseState.standing(),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }
}
