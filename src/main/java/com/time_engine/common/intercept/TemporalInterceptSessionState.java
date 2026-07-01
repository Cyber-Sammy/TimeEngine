package com.time_engine.common.intercept;

import com.time_engine.common.snapshot.EntitySnapshot;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

final class TemporalInterceptSessionState {
    private final Deque<TrackedBlock> blocks = new ArrayDeque<>();
    private final Map<UUID, TimelineSplice> splicesByTarget = new HashMap<>();
    private int lastServerTick;

    TemporalInterceptSessionState(int initialServerTick) {
        lastServerTick = initialServerTick;
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

    OptionalInt advance(int currentServerTick) {
        int previousServerTick = lastServerTick;
        if (currentServerTick <= previousServerTick) {
            return OptionalInt.empty();
        }
        lastServerTick = currentServerTick;
        return OptionalInt.of(previousServerTick);
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

    void recordSplice(
            UUID targetId,
            double collapsePerceivedTick,
            int correctionServerTick,
            EntitySnapshot fallbackSnapshot) {
        splicesByTarget.put(
                targetId,
                new TimelineSplice(collapsePerceivedTick, correctionServerTick, fallbackSnapshot));
    }

    boolean hasSplice(UUID targetId) {
        return splicesByTarget.containsKey(targetId);
    }

    Optional<TimelineSplice> splice(UUID targetId, double perceivedTick) {
        TimelineSplice splice = splicesByTarget.get(targetId);
        if (splice == null) {
            return Optional.empty();
        }
        if (perceivedTick < splice.collapsePerceivedTick()) {
            return Optional.empty();
        }
        return Optional.of(splice);
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

    record TimelineSplice(
            double collapsePerceivedTick,
            int correctionServerTick,
            EntitySnapshot fallbackSnapshot) {
        double mappedServerTick(double perceivedTick) {
            return correctionServerTick + Math.max(0.0D, perceivedTick - collapsePerceivedTick);
        }
    }
}
