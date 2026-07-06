package com.time_engine.engine.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.time_engine.engine.common.causal.CausalLink;
import com.time_engine.engine.common.causal.CausalLinkState;
import com.time_engine.engine.common.causal.CausalPhantomActionState;
import com.time_engine.engine.common.causal.CausalPhantomEquipmentState;
import com.time_engine.engine.common.causal.CausalPhantomFrame;
import com.time_engine.engine.common.causal.CausalPhantomPoseState;
import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkFrameBroadcasterTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Test
    void pursuitRenderStateUsesCurrentTargetAnchorAndCausalProgress() {
        CausalLink link =
                new CausalLink(
                        OWNER_ID,
                        TARGET_ID,
                        CausalLinkState.SOFT_LOCK,
                        0,
                        10,
                        0,
                        frame(TARGET_ID, new Vec3(10.0D, 0.0D, 0.0D)),
                        frame(TARGET_ID, new Vec3(10.0D, 0.0D, 0.0D)),
                        frame(OWNER_ID, Vec3.ZERO),
                        frame(OWNER_ID, new Vec3(100.0D, 0.0D, 0.0D)),
                        0.5D);

        TemporalEntityRenderState state =
                CausalLinkFrameBroadcaster.pursuitRenderState(link, new Vec3(20.0D, 0.0D, 0.0D));

        assertEquals(10.0D, state.position().x, 0.0001D);
        assertEquals(9.5D, state.boundingBox().minX, 0.0001D);
        assertEquals(10.5D, state.boundingBox().maxX, 0.0001D);
    }

    private static CausalPhantomFrame frame(UUID entityId, Vec3 anchor) {
        return new CausalPhantomFrame(
                entityId,
                10,
                anchor,
                boundsAt(anchor),
                CausalPhantomPoseState.standing(),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }

    private static AABB boundsAt(Vec3 anchor) {
        return new AABB(
                anchor.x - 0.5D,
                anchor.y,
                anchor.z - 0.5D,
                anchor.x + 0.5D,
                anchor.y + 1.8D,
                anchor.z + 0.5D);
    }
}
