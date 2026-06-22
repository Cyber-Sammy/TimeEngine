package com.time_engine.common.intercept;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

final class TemporalInterceptSessionState {
    private final Deque<TrackedBlock> blocks = new ArrayDeque<>();
    private double lastPerceivedTick;

    TemporalInterceptSessionState(double initialPerceivedTick) {
        lastPerceivedTick = initialPerceivedTick;
    }

    void add(PlacedBlockRecord record, int maxBlocks) {
        blocks.removeIf(block -> block.record().position().equals(record.position()));
        blocks.addLast(new TrackedBlock(record));
        trim(maxBlocks);
    }

    void trim(int maxBlocks) {
        while (blocks.size() > maxBlocks) {
            blocks.removeFirst();
        }
    }

    void removeInvalid(ServerLevel level) {
        blocks.removeIf(block -> !block.record().stillExists(level));
    }

    OptionalDouble advance(double currentPerceivedTick) {
        double previousPerceivedTick = lastPerceivedTick;
        lastPerceivedTick = currentPerceivedTick;
        if (currentPerceivedTick <= previousPerceivedTick) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(previousPerceivedTick);
    }

    boolean isEmpty() {
        return blocks.isEmpty();
    }

    List<TrackedBlock> blocks() {
        return List.copyOf(blocks);
    }

    int blockCount() {
        return blocks.size();
    }

    int interceptedTargetCount() {
        Set<UUID> targetIds = new HashSet<>();
        blocks.forEach(block -> targetIds.addAll(block.interceptedTargets()));
        return targetIds.size();
    }

    static final class TrackedBlock {
        private final PlacedBlockRecord record;
        private final Set<UUID> interceptedTargets = new HashSet<>();

        private TrackedBlock(PlacedBlockRecord record) {
            this.record = record;
        }

        PlacedBlockRecord record() {
            return record;
        }

        boolean hasIntercepted(UUID targetId) {
            return interceptedTargets.contains(targetId);
        }

        void markIntercepted(UUID targetId) {
            interceptedTargets.add(targetId);
        }

        Set<UUID> interceptedTargets() {
            return Set.copyOf(interceptedTargets);
        }
    }
}
