package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.time_engine.engine.common.snapshot.EntitySnapshot;
import java.util.UUID;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalPhantomFrameTest {
    private static final UUID ENTITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test
    void fromSnapshotUsesAuthoritativeSnapshotAnchorAndBounds() {
        EntitySnapshot snapshot =
                new EntitySnapshot(
                        ENTITY_ID,
                        42,
                        Level.OVERWORLD,
                        new Vec3(1.0D, 2.0D, 3.0D),
                        new Vec3(0.0D, 0.0D, 1.0D),
                        180.0F,
                        15.0F,
                        Pose.CROUCHING,
                        bounds(),
                        true,
                        20.0F);

        CausalPhantomFrame frame = CausalPhantomFrame.fromSnapshot(snapshot);

        assertEquals(ENTITY_ID, frame.entityId());
        assertEquals(42, frame.serverTick());
        assertEquals(snapshot.position(), frame.stableAnchor());
        assertEquals(bounds(), frame.authoritativeBounds());
        assertEquals(Pose.CROUCHING, frame.pose().pose());
        assertEquals(CausalPhantomActionState.NONE, frame.action());
        assertEquals(CausalPhantomEquipmentState.EMPTY, frame.equipment());
    }

    @Test
    void fromSnapshotBuildsMovementIntentFromHorizontalVelocity() {
        EntitySnapshot snapshot =
                new EntitySnapshot(
                        ENTITY_ID,
                        42,
                        Level.OVERWORLD,
                        Vec3.ZERO,
                        new Vec3(3.0D, 4.0D, 0.0D),
                        0.0F,
                        0.0F,
                        Pose.STANDING,
                        bounds(),
                        true,
                        20.0F);

        CausalPhantomFrame frame = CausalPhantomFrame.fromSnapshot(snapshot);

        assertEquals(new Vec3(1.0D, 0.0D, 0.0D), frame.pose().movementIntent());
    }

    private static AABB bounds() {
        return new AABB(0.5D, 1.0D, 2.5D, 1.5D, 2.8D, 3.5D);
    }
}
