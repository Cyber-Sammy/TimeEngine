package com.time_engine.engine.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.engine.common.snapshot.EntitySnapshot;
import java.util.UUID;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GhostFrameBoundaryTest {
    @Test
    void leavesSnapshotsInsideRadiusUnchanged() {
        EntitySnapshot snapshot = snapshotAt(new Vec3(3.0D, 0.0D, 0.0D));

        EntitySnapshot actual = GhostFrameBoundary.clampToRadius(snapshot, Vec3.ZERO, 5.0D);

        assertEquals(snapshot, actual);
        assertTrue(GhostFrameBoundary.canRenderAtBoundary(snapshot, Vec3.ZERO, 5.0D));
    }

    @Test
    void clampsSnapshotCenterAndBoundsToRadius() {
        EntitySnapshot snapshot = snapshotAt(new Vec3(7.0D, 0.0D, 0.0D));

        EntitySnapshot actual = GhostFrameBoundary.clampToRadius(snapshot, Vec3.ZERO, 5.0D);

        assertEquals(new Vec3(5.0D, 0.0D, 0.0D), actual.position());
        assertEquals(4.5D, actual.boundingBox().minX, 0.0001D);
        assertEquals(5.5D, actual.boundingBox().maxX, 0.0001D);
    }

    @Test
    void stopsRenderingAfterBoundsFullyPassBoundaryGrace() {
        EntitySnapshot nearBoundary = snapshotAt(new Vec3(5.4D, 0.0D, 0.0D));
        EntitySnapshot farOutside = snapshotAt(new Vec3(6.0D, 0.0D, 0.0D));

        assertTrue(GhostFrameBoundary.canRenderAtBoundary(nearBoundary, Vec3.ZERO, 5.0D));
        assertFalse(GhostFrameBoundary.canRenderAtBoundary(farOutside, Vec3.ZERO, 5.0D));
    }

    private static EntitySnapshot snapshotAt(Vec3 position) {
        AABB bounds =
                new AABB(
                        position.x - 0.5D,
                        position.y,
                        position.z - 0.5D,
                        position.x + 0.5D,
                        position.y + 1.0D,
                        position.z + 0.5D);
        return new EntitySnapshot(
                UUID.randomUUID(),
                100,
                Level.OVERWORLD,
                position,
                Vec3.ZERO,
                0.0F,
                0.0F,
                Pose.STANDING,
                bounds,
                true,
                EntitySnapshot.UNKNOWN_HEALTH);
    }
}
