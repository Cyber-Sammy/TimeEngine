package com.time_engine.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.time_engine.common.network.GhostFramePayload.GhostEntityState;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GhostFramePayloadTest {
    private static final UUID ENTITY_ID = UUID.fromString("dbfd788c-cd87-4f8f-a6fc-0c5f38357f5e");

    @Test
    void interpolatesPositionBoundsAndWrappedRotation() {
        GhostEntityState previous = state(0.0D, 170.0F);
        GhostEntityState current = state(10.0D, -170.0F);

        GhostEntityState interpolated = previous.interpolate(current, 0.5D);

        assertEquals(5.0D, interpolated.position().x, 0.0001D);
        assertEquals(5.0D, interpolated.boundingBox().minX, 0.0001D);
        assertEquals(180.0F, Math.abs(interpolated.yRot()), 0.0001F);
    }

    @Test
    void payloadCopiesEntityList() {
        var entities = new java.util.ArrayList<>(List.of(state(0.0D, 0.0F)));
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

    private static GhostEntityState state(double x, float yRot) {
        return new GhostEntityState(
                ENTITY_ID,
                new Vec3(x, 2.0D, 3.0D),
                yRot,
                0.0F,
                Pose.STANDING,
                new AABB(x, 2.0D, 3.0D, x + 1.0D, 4.0D, 4.0D));
    }
}
