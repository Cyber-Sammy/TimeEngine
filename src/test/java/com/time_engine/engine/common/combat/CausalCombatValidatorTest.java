package com.time_engine.engine.common.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.engine.common.causal.CausalLink;
import com.time_engine.engine.common.causal.CausalLinkManager;
import com.time_engine.engine.common.causal.CausalLinkState;
import com.time_engine.engine.common.causal.CausalPhantomActionState;
import com.time_engine.engine.common.causal.CausalPhantomEquipmentState;
import com.time_engine.engine.common.causal.CausalPhantomFrame;
import com.time_engine.engine.common.causal.CausalPhantomPoseState;
import com.time_engine.engine.common.combat.CausalCombatValidator.CausalCombatRejectionReason;
import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalCombatValidatorTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_TARGET_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void rejectsMissingCausalLink() {
        CausalLinkManager manager = new CausalLinkManager();

        CausalCombatValidator.CausalCombatValidationResult result =
                CausalCombatValidator.validate(manager, OWNER_ID, TARGET_ID);

        assertEquals(CausalCombatRejectionReason.NO_LINK, result.rejectionReason());
    }

    @Test
    void rejectsMismatchedCausalTarget() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.putLink(activeLink(TARGET_ID));

        CausalCombatValidator.CausalCombatValidationResult result =
                CausalCombatValidator.validate(manager, OWNER_ID, OTHER_TARGET_ID);

        assertEquals(CausalCombatRejectionReason.TARGET_MISMATCH, result.rejectionReason());
    }

    @Test
    void rejectsBrokenCausalLink() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.putLink(activeLink(TARGET_ID).breakLink(10));

        CausalCombatValidator.CausalCombatValidationResult result =
                CausalCombatValidator.validate(manager, OWNER_ID, TARGET_ID);

        assertEquals(CausalCombatRejectionReason.BROKEN, result.rejectionReason());
    }

    @Test
    void rejectsExpiredCausalLink() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.putLink(activeLink(TARGET_ID).expire(10));

        CausalCombatValidator.CausalCombatValidationResult result =
                CausalCombatValidator.validate(manager, OWNER_ID, TARGET_ID);

        assertEquals(CausalCombatRejectionReason.EXPIRED, result.rejectionReason());
    }

    @Test
    void acceptsMatchingActiveCausalLink() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.putLink(activeLink(TARGET_ID));

        CausalCombatValidator.CausalCombatValidationResult result =
                CausalCombatValidator.validate(manager, OWNER_ID, TARGET_ID);

        assertTrue(result.accepted());
    }

    @Test
    void hardLocksAcceptedHit() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.putLink(activeLink(TARGET_ID));

        CausalLink link =
                CausalCombatValidator.hardLockAcceptedHit(manager, OWNER_ID, 15).orElseThrow();

        assertEquals(CausalLinkState.HARD_LOCK, link.state());
        assertTrue(link.hardLockUntilTick() > 15);
    }

    private static CausalLink activeLink(UUID targetId) {
        return CausalLink.softLocked(OWNER_ID, targetId, 1, frame(targetId), frame(OWNER_ID));
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
