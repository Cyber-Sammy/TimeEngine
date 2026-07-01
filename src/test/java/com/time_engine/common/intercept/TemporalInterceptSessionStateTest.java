package com.time_engine.common.intercept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.common.snapshot.EntitySnapshot;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class TemporalInterceptSessionStateTest {
    @Test
    void keepsOnlyNewestConfiguredBlocks() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10);
        state.add(record(0), 2);
        state.add(record(1), 2);
        state.add(record(2), 2);

        assertEquals(2, state.blockCount());
        assertEquals(new BlockPos(1, 0, 0), state.blocks().getFirst().record().position());
    }

    @Test
    void replacesRecordAtSamePosition() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10);
        state.add(record(0), 4);
        state.blocks().getFirst().markIntercepted(UUID.randomUUID());
        state.add(record(0), 4);

        assertEquals(1, state.blockCount());
        assertEquals(0, state.interceptedTargetCount());
    }

    @Test
    void advancesOnlyForIncreasingServerTime() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10);

        assertFalse(state.advance(10).isPresent());
        assertEquals(10, state.advance(11).orElseThrow());
        assertFalse(state.advance(10).isPresent());
        assertEquals(11, state.advance(12).orElseThrow());
    }

    @Test
    void tracksInterceptedTargetOncePerBlock() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10);
        state.add(record(0), 4);
        UUID targetId = UUID.randomUUID();

        state.blocks().getFirst().markIntercepted(targetId);

        assertTrue(state.blocks().getFirst().hasIntercepted(targetId));
        assertEquals(1, state.interceptedTargetCount());
    }

    @Test
    void returnsTimelineSpliceAtAndAfterCollapseTick() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10);
        UUID targetId = UUID.randomUUID();
        EntitySnapshot snapshot = snapshot(targetId, 20, 1.0D);

        state.recordSplice(targetId, 25.5D, 40, snapshot);

        assertTrue(state.hasSplice(targetId));
        assertFalse(state.splice(targetId, 25.49D).isPresent());

        TemporalInterceptSessionState.TimelineSplice splice =
                state.splice(targetId, 25.5D).orElseThrow();
        assertEquals(snapshot, splice.fallbackSnapshot());
        assertEquals(40.0D, splice.mappedServerTick(25.5D), 0.0001D);
        assertEquals(44.5D, splice.mappedServerTick(30.0D), 0.0001D);
    }

    @Test
    void replacesTimelineSpliceForRepeatedIntercepts() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10);
        UUID targetId = UUID.randomUUID();
        EntitySnapshot first = snapshot(targetId, 20, 1.0D);
        EntitySnapshot second = snapshot(targetId, 40, 5.0D);

        state.recordSplice(targetId, 25.0D, 40, first);
        state.recordSplice(targetId, 45.0D, 60, second);

        assertFalse(state.splice(targetId, 44.99D).isPresent());

        TemporalInterceptSessionState.TimelineSplice splice =
                state.splice(targetId, 45.0D).orElseThrow();
        assertEquals(second, splice.fallbackSnapshot());
        assertEquals(60.0D, splice.mappedServerTick(45.0D), 0.0001D);
    }

    private static PlacedBlockRecord record(int x) {
        BlockPos position = new BlockPos(x, 0, 0);
        return new PlacedBlockRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Level.OVERWORLD,
                position,
                Blocks.STONE.defaultBlockState(),
                20,
                List.of(new AABB(position)));
    }

    private static EntitySnapshot snapshot(UUID entityId, int tick, double x) {
        return new EntitySnapshot(
                entityId,
                tick,
                Level.OVERWORLD,
                new Vec3(x, 2.0D, 3.0D),
                Vec3.ZERO,
                0.0F,
                0.0F,
                Pose.STANDING,
                new AABB(x, 2.0D, 3.0D, x + 1.0D, 4.0D, 4.0D),
                true,
                20.0F);
    }
}
