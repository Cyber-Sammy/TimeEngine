package com.time_engine.engine.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.time_engine.engine.common.causal.CausalLinkState;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkFramePayloadTest {
    @Test
    void codecRoundTripsAllValues() {
        UUID ownerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        CausalLinkFramePayload expected =
                new CausalLinkFramePayload(
                        ownerId,
                        targetId,
                        CausalLinkState.SOFT_LOCK,
                        120,
                        0.75D,
                        ResourceLocation.withDefaultNamespace("overworld"),
                        state(ownerId, 1.0D),
                        state(targetId, 2.0D),
                        state(ownerId, 3.0D),
                        state(targetId, 4.0D));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CausalLinkFramePayload.STREAM_CODEC.encode(buffer, expected);

            assertEquals(expected, CausalLinkFramePayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    private static TemporalEntityRenderState state(UUID entityId, double x) {
        return new TemporalEntityRenderState(
                entityId,
                new Vec3(x, 2.0D, 3.0D),
                45.0F,
                -10.0F,
                Pose.STANDING,
                new AABB(x, 2.0D, 3.0D, x + 1.0D, 4.0D, 4.0D),
                120.0D,
                true);
    }
}
