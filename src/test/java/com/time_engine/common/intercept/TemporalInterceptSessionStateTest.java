package com.time_engine.common.intercept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class TemporalInterceptSessionStateTest {
    @Test
    void keepsOnlyNewestConfiguredBlocks() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10.0D);
        state.add(record(0), 2);
        state.add(record(1), 2);
        state.add(record(2), 2);

        assertEquals(2, state.blockCount());
        assertEquals(new BlockPos(1, 0, 0), state.blocks().getFirst().record().position());
    }

    @Test
    void replacesRecordAtSamePosition() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10.0D);
        state.add(record(0), 4);
        state.blocks().getFirst().markIntercepted(UUID.randomUUID());
        state.add(record(0), 4);

        assertEquals(1, state.blockCount());
        assertEquals(0, state.interceptedTargetCount());
    }

    @Test
    void advancesOnlyForIncreasingPerceivedTime() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10.0D);

        assertFalse(state.advance(10.0D).isPresent());
        assertEquals(10.0D, state.advance(10.5D).orElseThrow(), 0.0001D);
        assertFalse(state.advance(10.25D).isPresent());
        assertEquals(10.25D, state.advance(11.0D).orElseThrow(), 0.0001D);
    }

    @Test
    void tracksInterceptedTargetOncePerBlock() {
        TemporalInterceptSessionState state = new TemporalInterceptSessionState(10.0D);
        state.add(record(0), 4);
        UUID targetId = UUID.randomUUID();

        state.blocks().getFirst().markIntercepted(targetId);

        assertTrue(state.blocks().getFirst().hasIntercepted(targetId));
        assertEquals(1, state.interceptedTargetCount());
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
}
