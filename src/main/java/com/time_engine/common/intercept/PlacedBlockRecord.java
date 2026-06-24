package com.time_engine.common.intercept;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public record PlacedBlockRecord(
        UUID sessionId,
        UUID placedBy,
        ResourceKey<Level> dimension,
        BlockPos position,
        BlockState blockState,
        int placedAtTick,
        List<AABB> collisionBoxes) {
    public PlacedBlockRecord {
        position = position.immutable();
        collisionBoxes = List.copyOf(collisionBoxes);
    }

    public static Optional<PlacedBlockRecord> create(
            UUID sessionId,
            UUID placedBy,
            ServerLevel level,
            BlockPos position,
            BlockState blockState,
            int placedAtTick) {
        List<AABB> collisionBoxes =
                blockState.getCollisionShape(level, position).toAabbs().stream()
                        .map(box -> box.move(position))
                        .toList();
        if (blockState.isAir()) {
            return Optional.empty();
        }
        return Optional.of(
                new PlacedBlockRecord(
                        sessionId,
                        placedBy,
                        level.dimension(),
                        position,
                        blockState,
                        placedAtTick,
                        collisionBoxes));
    }

    public boolean stillExists(ServerLevel level) {
        if (!level.dimension().equals(dimension)) {
            return false;
        }
        return level.getBlockState(position).equals(blockState);
    }

    public boolean intersects(AABB bounds) {
        for (AABB collisionBox : collisionBoxes) {
            if (collisionBox.intersects(bounds)) {
                return true;
            }
        }
        return false;
    }
}
