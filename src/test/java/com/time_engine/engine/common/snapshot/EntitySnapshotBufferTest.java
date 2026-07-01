package com.time_engine.engine.common.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EntitySnapshotBufferTest {
    private static final UUID ENTITY_ID = UUID.fromString("e5f4aa17-705f-4ee5-a612-3f9feba5530f");

    @Test
    void overwritesSnapshotsOutsideCapacity() {
        EntitySnapshotBuffer buffer = new EntitySnapshotBuffer(ENTITY_ID, 3);

        buffer.addSnapshot(snapshot(10, 0.0D, 20.0F));
        buffer.addSnapshot(snapshot(11, 1.0D, 19.0F));
        buffer.addSnapshot(snapshot(12, 2.0D, 18.0F));
        buffer.addSnapshot(snapshot(13, 3.0D, 17.0F));

        assertEquals(3, buffer.size());
        assertFalse(buffer.getSnapshotAtTick(10).isPresent());
        assertEquals(3.0D, buffer.getSnapshotAtTick(13).orElseThrow().position().x);
    }

    @Test
    void interpolatesPositionHealthAndWrappedRotation() {
        EntitySnapshotBuffer buffer = new EntitySnapshotBuffer(ENTITY_ID, 4);
        buffer.addSnapshot(snapshot(20, 0.0D, 20.0F, 170.0F));
        buffer.addSnapshot(snapshot(21, 10.0D, 10.0F, -170.0F));

        EntitySnapshot interpolated = buffer.getInterpolatedSnapshot(20.5D).orElseThrow();

        assertEquals(5.0D, interpolated.position().x, 0.0001D);
        assertEquals(15.0F, interpolated.health(), 0.0001F);
        assertEquals(180.0F, Math.abs(interpolated.yRot()), 0.0001F);
    }

    @Test
    void rejectsOutOfOrderSnapshots() {
        EntitySnapshotBuffer buffer = new EntitySnapshotBuffer(ENTITY_ID, 3);
        buffer.addSnapshot(snapshot(30, 0.0D, 20.0F));

        assertThrows(
                IllegalArgumentException.class,
                () -> buffer.addSnapshot(snapshot(29, 0.0D, 20.0F)));
    }

    @Test
    void returnsAvailableNeighbourWhenOneInterpolationEndpointIsMissing() {
        EntitySnapshotBuffer buffer = new EntitySnapshotBuffer(ENTITY_ID, 3);
        buffer.addSnapshot(snapshot(41, 4.0D, 20.0F));

        assertTrue(buffer.getInterpolatedSnapshot(40.5D).isPresent());
        assertEquals(41, buffer.getInterpolatedSnapshot(40.5D).orElseThrow().serverTick());
    }

    private static EntitySnapshot snapshot(int tick, double x, float health) {
        return snapshot(tick, x, health, 0.0F);
    }

    private static EntitySnapshot snapshot(int tick, double x, float health, float yRot) {
        return new EntitySnapshot(
                ENTITY_ID,
                tick,
                Level.OVERWORLD,
                new Vec3(x, 2.0D, 3.0D),
                new Vec3(1.0D, 0.0D, 0.0D),
                yRot,
                0.0F,
                Pose.STANDING,
                new AABB(x, 2.0D, 3.0D, x + 1.0D, 4.0D, 4.0D),
                true,
                health);
    }
}
