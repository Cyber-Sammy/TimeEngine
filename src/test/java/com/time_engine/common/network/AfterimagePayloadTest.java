package com.time_engine.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AfterimagePayloadTest {
    @Test
    void codecRoundTripsAllValues() {
        AfterimagePayload expected =
                new AfterimagePayload(
                        UUID.randomUUID(),
                        42,
                        ResourceLocation.withDefaultNamespace("overworld"),
                        12,
                        new TemporalEntityRenderState(
                                UUID.randomUUID(),
                                new Vec3(1.0D, 2.0D, 3.0D),
                                45.0F,
                                -10.0F,
                                Pose.CROUCHING,
                                new AABB(0.5D, 2.0D, 2.5D, 1.5D, 3.8D, 3.5D),
                                42.0D,
                                true));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AfterimagePayload.STREAM_CODEC.encode(buffer, expected);

            assertEquals(expected, AfterimagePayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
