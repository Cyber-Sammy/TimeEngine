package com.time_engine.engine.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GhostFramePayloadTest {
    private static final UUID ENTITY_ID = UUID.fromString("dbfd788c-cd87-4f8f-a6fc-0c5f38357f5e");

    @Test
    void interpolatesPositionBoundsAndWrappedRotation() {
        TemporalEntityRenderState previous = state(0.0D, 170.0F, Pose.STANDING);
        TemporalEntityRenderState current = state(10.0D, -170.0F, Pose.STANDING);

        TemporalEntityRenderState interpolated = previous.interpolate(current, 0.5D);

        assertEquals(5.0D, interpolated.position().x, 0.0001D);
        assertEquals(5.0D, interpolated.boundingBox().minX, 0.0001D);
        assertEquals(180.0F, Math.abs(interpolated.yRot()), 0.0001F);
    }

    @Test
    void payloadCopiesEntityList() {
        var entities = new java.util.ArrayList<>(List.of(state(0.0D, 0.0F, Pose.STANDING)));
        GhostFramePayload payload =
                new GhostFramePayload(
                        UUID.randomUUID(),
                        10,
                        5.0D,
                        ResourceLocation.withDefaultNamespace("overworld"),
                        entities);

        entities.clear();

        assertEquals(1, payload.entities().size());
        assertThrows(UnsupportedOperationException.class, () -> payload.entities().clear());
    }

    @Test
    void roundTripsSwimmingPose() {
        assertEquals(Pose.SWIMMING, roundTrip(Pose.SWIMMING));
    }

    @Test
    void roundTripsCrouchingPose() {
        assertEquals(Pose.CROUCHING, roundTrip(Pose.CROUCHING));
    }

    @Test
    void fallsBackToStandingForUnknownPose() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeState(buffer, "UNKNOWN_FUTURE_POSE", true);

            assertEquals(
                    Pose.STANDING, TemporalEntityRenderState.STREAM_CODEC.decode(buffer).pose());
        } finally {
            buffer.release();
        }
    }

    @Test
    void codecRoundTripsPhantomCombatFlag() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TemporalEntityRenderState.STREAM_CODEC.encode(
                    buffer, state(0.0D, 0.0F, Pose.STANDING, false));

            assertEquals(
                    false,
                    TemporalEntityRenderState.STREAM_CODEC.decode(buffer).phantomCombatAllowed());
        } finally {
            buffer.release();
        }
    }

    @Test
    void codecRoundTripsPerEntityPerceivedTick() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TemporalEntityRenderState.STREAM_CODEC.encode(
                    buffer, state(0.0D, 0.0F, Pose.STANDING, true, 123.5D));

            assertEquals(
                    123.5D,
                    TemporalEntityRenderState.STREAM_CODEC.decode(buffer).perceivedTick(),
                    0.0001D);
        } finally {
            buffer.release();
        }
    }

    @Test
    void interpolationKeepsPhantomCombatDisabledWhenEitherFrameDisablesIt() {
        TemporalEntityRenderState previous = state(0.0D, 0.0F, Pose.STANDING, true, 100.0D);
        TemporalEntityRenderState current = state(10.0D, 0.0F, Pose.STANDING, false, 110.0D);

        TemporalEntityRenderState interpolated = previous.interpolate(current, 0.5D);

        assertEquals(false, interpolated.phantomCombatAllowed());
        assertEquals(105.0D, interpolated.perceivedTick(), 0.0001D);
    }

    private static Pose roundTrip(Pose pose) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TemporalEntityRenderState.STREAM_CODEC.encode(buffer, state(0.0D, 0.0F, pose));
            return TemporalEntityRenderState.STREAM_CODEC.decode(buffer).pose();
        } finally {
            buffer.release();
        }
    }

    private static void writeState(
            FriendlyByteBuf buffer, String poseName, boolean phantomCombatAllowed) {
        buffer.writeUUID(ENTITY_ID);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(2.0D);
        buffer.writeDouble(3.0D);
        buffer.writeFloat(0.0F);
        buffer.writeFloat(0.0F);
        buffer.writeUtf(poseName, 64);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(2.0D);
        buffer.writeDouble(3.0D);
        buffer.writeDouble(1.0D);
        buffer.writeDouble(4.0D);
        buffer.writeDouble(4.0D);
        buffer.writeDouble(0.0D);
        buffer.writeBoolean(phantomCombatAllowed);
    }

    private static TemporalEntityRenderState state(double x, float yRot, Pose pose) {
        return state(x, yRot, pose, true);
    }

    private static TemporalEntityRenderState state(
            double x, float yRot, Pose pose, boolean phantomCombatAllowed) {
        return state(x, yRot, pose, phantomCombatAllowed, 0.0D);
    }

    private static TemporalEntityRenderState state(
            double x, float yRot, Pose pose, boolean phantomCombatAllowed, double perceivedTick) {
        return new TemporalEntityRenderState(
                ENTITY_ID,
                new Vec3(x, 2.0D, 3.0D),
                yRot,
                0.0F,
                pose,
                new AABB(x, 2.0D, 3.0D, x + 1.0D, 4.0D, 4.0D),
                perceivedTick,
                phantomCombatAllowed);
    }
}
