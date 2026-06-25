package com.time_engine.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.common.network.AfterimagePayload;
import com.time_engine.common.network.TemporalEntityRenderState;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AfterimageTrailTest {
    private static final UUID PLAYER_ID = UUID.fromString("ea1b26dc-e3f2-4fd8-a3b4-c5ce09cf1201");

    @Test
    void replacesAnchorsWhenSessionChanges() {
        AfterimageTrail trail = new AfterimageTrail();
        trail.add(payload(UUID.randomUUID(), 10, 20), 100L);
        trail.add(payload(UUID.randomUUID(), 1, 20), 101L);

        assertEquals(1, trail.size());
    }

    @Test
    void ignoresOutOfOrderAnchors() {
        UUID sessionId = UUID.randomUUID();
        AfterimageTrail trail = new AfterimageTrail();
        trail.add(payload(sessionId, 10, 20), 100L);
        trail.add(payload(sessionId, 9, 20), 101L);

        assertEquals(1, trail.size());
    }

    @Test
    void ignoresAnchorsAtTheSamePosition() {
        UUID sessionId = UUID.randomUUID();
        AfterimageTrail trail = new AfterimageTrail();
        trail.add(payload(sessionId, 10, 20), 100L);
        trail.add(payload(sessionId, 11, 20), 101L);

        assertEquals(1, trail.size());
    }

    @Test
    void prunesExpiredAnchors() {
        AfterimageTrail trail = new AfterimageTrail();
        trail.add(payload(UUID.randomUUID(), 10, 5), 100L);

        trail.prune(105L);

        assertTrue(trail.isEmpty());
    }

    @Test
    void fadesAnchorOverItsLifetime() {
        AfterimageTrail trail = new AfterimageTrail();
        trail.add(payload(UUID.randomUUID(), 10, 10), 100L);

        float alpha = trail.getRenderStates(105L, 0.0F).getFirst().alpha();

        assertEquals(0.5F, alpha, 0.0001F);
    }

    private static AfterimagePayload payload(UUID sessionId, int serverTick, int lifetimeTicks) {
        TemporalEntityRenderState state =
                new TemporalEntityRenderState(
                        PLAYER_ID,
                        new Vec3(1.0D, 2.0D, 3.0D),
                        0.0F,
                        0.0F,
                        Pose.STANDING,
                        new AABB(0.5D, 2.0D, 2.5D, 1.5D, 3.8D, 3.5D),
                        true);
        return new AfterimagePayload(
                sessionId,
                serverTick,
                ResourceLocation.withDefaultNamespace("overworld"),
                lifetimeTicks,
                state);
    }
}
