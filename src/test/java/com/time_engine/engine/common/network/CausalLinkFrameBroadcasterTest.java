package com.time_engine.engine.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.time_engine.engine.common.causal.CausalLink;
import com.time_engine.engine.common.causal.CausalLinkState;
import com.time_engine.engine.common.causal.CausalPhantomActionState;
import com.time_engine.engine.common.causal.CausalPhantomEquipmentState;
import com.time_engine.engine.common.causal.CausalPhantomFrame;
import com.time_engine.engine.common.causal.CausalPhantomPoseState;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkFrameBroadcasterTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.withDefaultNamespace("overworld");

    @Test
    void pursuitRenderStateUsesCurrentTargetAnchorAndCausalProgress() {
        CausalLink link =
                linkBuilder()
                        .targetAt(new Vec3(10.0D, 0.0D, 0.0D))
                        .ownerOriginAt(Vec3.ZERO)
                        .ownerLatestAt(new Vec3(100.0D, 0.0D, 0.0D))
                        .progress(0.5D)
                        .build();

        TemporalEntityRenderState state =
                CausalLinkFrameBroadcaster.pursuitRenderState(link, new Vec3(20.0D, 0.0D, 0.0D));

        assertEquals(10.0D, state.position().x, 0.0001D);
        assertEquals(9.5D, state.boundingBox().minX, 0.0001D);
        assertEquals(10.5D, state.boundingBox().maxX, 0.0001D);
    }

    @Test
    void payloadCanBeBuiltForNonPlayerTargetDebugState() {
        CausalLink link =
                linkBuilder()
                        .targetAt(new Vec3(8.0D, 0.0D, 0.0D))
                        .ownerOriginAt(Vec3.ZERO)
                        .ownerLatestAt(Vec3.ZERO)
                        .progress(0.25D)
                        .build();

        CausalLinkFramePayload payload =
                CausalLinkFrameBroadcaster.payloadFor(
                        link, OVERWORLD, new Vec3(12.0D, 0.0D, 0.0D), 42);

        assertEquals(OWNER_ID, payload.ownerId());
        assertEquals(TARGET_ID, payload.targetId());
        assertEquals(OVERWORLD, payload.dimension());
        assertEquals(42, payload.serverTick());
        assertEquals(3.0D, payload.pursuitFrame().position().x, 0.0001D);
        assertEquals(8.0D, payload.targetFrame().position().x, 0.0001D);
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

    private static LinkBuilder linkBuilder() {
        return new LinkBuilder();
    }

    private static final class LinkBuilder {
        private Vec3 targetAnchor = Vec3.ZERO;
        private Vec3 ownerOriginAnchor = Vec3.ZERO;
        private Vec3 ownerLatestAnchor = Vec3.ZERO;
        private double progress;

        private LinkBuilder targetAt(Vec3 anchor) {
            targetAnchor = anchor;
            return this;
        }

        private LinkBuilder ownerOriginAt(Vec3 anchor) {
            ownerOriginAnchor = anchor;
            return this;
        }

        private LinkBuilder ownerLatestAt(Vec3 anchor) {
            ownerLatestAnchor = anchor;
            return this;
        }

        private LinkBuilder progress(double value) {
            progress = value;
            return this;
        }

        private CausalLink build() {
            return new CausalLink(
                    OWNER_ID,
                    TARGET_ID,
                    CausalLinkState.SOFT_LOCK,
                    0,
                    10,
                    0,
                    frame(TARGET_ID, targetAnchor),
                    frame(TARGET_ID, targetAnchor),
                    frame(OWNER_ID, ownerOriginAnchor),
                    frame(OWNER_ID, ownerLatestAnchor),
                    progress);
        }
    }
}
