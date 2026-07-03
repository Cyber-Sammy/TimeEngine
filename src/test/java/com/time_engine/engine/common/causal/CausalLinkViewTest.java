package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkViewTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Test
    void fromLinkExposesDerivedDiagnostics() {
        CausalLink link = CausalLink.softLocked(OWNER_ID, TARGET_ID, 10, frame()).hardLock(12, 20);

        CausalLinkView view = CausalLinkView.from(link, 15);

        assertEquals(OWNER_ID, view.ownerId());
        assertEquals(TARGET_ID, view.targetId());
        assertEquals(CausalLinkState.HARD_LOCK, view.state());
        assertEquals(5, view.ageTicks());
        assertEquals(3, view.ticksSinceUpdate());
        assertEquals(17, view.hardLockTicksRemaining());
        assertTrue(view.active());
    }

    @Test
    void negativeDerivedValuesAreClamped() {
        CausalLinkView view =
                new CausalLinkView(
                        OWNER_ID, TARGET_ID, CausalLinkState.SOFT_LOCK, 10, 20, -1, -2, -3, 2.0D);

        assertEquals(0, view.ageTicks());
        assertEquals(0, view.ticksSinceUpdate());
        assertEquals(0, view.hardLockTicksRemaining());
        assertEquals(1.0D, view.progress());
    }

    private static CausalPhantomFrame frame() {
        return new CausalPhantomFrame(
                TARGET_ID,
                0,
                Vec3.ZERO,
                new AABB(-0.5D, 0.0D, -0.5D, 0.5D, 1.8D, 0.5D),
                CausalPhantomPoseState.standing(),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }
}
