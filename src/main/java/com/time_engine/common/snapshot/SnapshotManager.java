package com.time_engine.common.snapshot;

import com.time_engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.common.policy.TemporalPolicyResolver;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SnapshotManager {
    private static final SnapshotManager INSTANCE = new SnapshotManager();

    private final Map<UUID, EntitySnapshotBuffer> buffersByEntity = new HashMap<>();
    private int bufferCapacity;

    private SnapshotManager() {}

    public static SnapshotManager getInstance() {
        return INSTANCE;
    }

    public void tick(MinecraftServer server) {
        int currentTick = server.getTickCount();
        int configuredCapacity = TimeEngineConfig.snapshotHistoryTicks() + 1;
        if (bufferCapacity != configuredCapacity) {
            resetForCapacity(configuredCapacity);
        }

        Collection<TemporalSession> activeSessions =
                TemporalSessionManager.getInstance().getActiveSessions();
        boolean snapshotPlayersAlways = TimeEngineConfig.snapshotPlayersAlways();
        Set<UUID> capturedEntityIds = new HashSet<>();
        if (snapshotPlayersAlways) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                captureIfTracked(player, true, currentTick, capturedEntityIds);
            }
        }

        for (TemporalSession session : activeSessions) {
            ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerPlayerId());
            if (owner != null) {
                if (!snapshotPlayersAlways) {
                    captureIfTracked(owner, true, currentTick, capturedEntityIds);
                }
                captureNearbyEntities(
                        owner,
                        session.radius(),
                        !snapshotPlayersAlways,
                        currentTick,
                        capturedEntityIds);
            }
        }

        int oldestRetainedTick = currentTick - TimeEngineConfig.snapshotHistoryTicks();
        buffersByEntity
                .values()
                .removeIf(buffer -> buffer.latestRecordedTick() < oldestRetainedTick);

        if (shouldLogSnapshotState(activeSessions, currentTick)) {
            ModLog.diagnostic(
                    "Snapshot state at tick {}: captured={}, trackedBuffers={}",
                    currentTick,
                    capturedEntityIds.size(),
                    buffersByEntity.size());
        }
    }

    public Optional<EntitySnapshot> getSnapshot(UUID entityId, int serverTick) {
        EntitySnapshotBuffer buffer = buffersByEntity.get(entityId);
        return buffer == null ? Optional.empty() : buffer.getSnapshotAtTick(serverTick);
    }

    public Optional<EntitySnapshot> getInterpolatedSnapshot(UUID entityId, double serverTick) {
        EntitySnapshotBuffer buffer = buffersByEntity.get(entityId);
        return buffer == null ? Optional.empty() : buffer.getInterpolatedSnapshot(serverTick);
    }

    public List<EntitySnapshot> getInterpolatedSnapshots(
            ResourceKey<Level> dimension,
            Vec3 center,
            double radius,
            double serverTick,
            UUID excludedEntityId,
            int limit) {
        double radiusSquared = radius * radius;
        return buffersByEntity.values().stream()
                .filter(buffer -> !buffer.entityId().equals(excludedEntityId))
                .map(buffer -> buffer.getInterpolatedSnapshot(serverTick))
                .flatMap(Optional::stream)
                .filter(EntitySnapshot::alive)
                .filter(snapshot -> snapshot.dimension().equals(dimension))
                .filter(snapshot -> snapshot.position().distanceToSqr(center) <= radiusSquared)
                .sorted(
                        Comparator.comparingDouble(
                                snapshot -> snapshot.position().distanceToSqr(center)))
                .limit(limit)
                .toList();
    }

    public int trackedEntityCount() {
        return buffersByEntity.size();
    }

    public Optional<BufferStats> getBufferStats(UUID entityId) {
        EntitySnapshotBuffer buffer = buffersByEntity.get(entityId);
        if (buffer == null) {
            return Optional.empty();
        }
        return Optional.of(
                new BufferStats(buffer.size(), buffer.capacity(), buffer.latestRecordedTick()));
    }

    public void clear() {
        ModLog.diagnostic("Clearing {} entity snapshot buffers", buffersByEntity.size());
        buffersByEntity.clear();
        bufferCapacity = 0;
    }

    private void captureNearbyEntities(
            ServerPlayer owner,
            double radius,
            boolean includePlayers,
            int currentTick,
            Set<UUID> capturedEntityIds) {
        AABB searchBounds = owner.getBoundingBox().inflate(radius);
        double radiusSquared = radius * radius;
        List<Entity> candidates =
                owner.serverLevel()
                        .getEntities(
                                owner,
                                searchBounds,
                                entity ->
                                        isCandidate(entity, owner, includePlayers, radiusSquared));
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(owner)));

        int limit = Math.min(candidates.size(), TimeEngineConfig.maxTrackedEntitiesPerSession());
        for (int index = 0; index < limit; index++) {
            capture(candidates.get(index), currentTick, capturedEntityIds);
        }
    }

    private void capture(Entity entity, int currentTick, Set<UUID> capturedEntityIds) {
        if (!capturedEntityIds.add(entity.getUUID())) {
            return;
        }

        buffersByEntity
                .computeIfAbsent(
                        entity.getUUID(),
                        ignored -> new EntitySnapshotBuffer(entity.getUUID(), bufferCapacity))
                .addSnapshot(EntitySnapshot.capture(entity, currentTick));
    }

    private void captureIfTracked(
            Entity entity, boolean includePlayers, int currentTick, Set<UUID> capturedEntityIds) {
        if (!shouldTrack(entity, includePlayers)) {
            return;
        }
        capture(entity, currentTick, capturedEntityIds);
    }

    private void resetForCapacity(int configuredCapacity) {
        if (!buffersByEntity.isEmpty()) {
            ModLog.diagnostic(
                    "Snapshot history capacity changed from {} to {}; clearing existing history",
                    bufferCapacity,
                    configuredCapacity);
            buffersByEntity.clear();
        }
        bufferCapacity = configuredCapacity;
    }

    private static boolean shouldTrack(Entity entity, boolean includePlayers) {
        if (entity.isRemoved()) {
            return false;
        }
        if (shouldExcludePlayer(entity, includePlayers)) {
            return false;
        }
        Decision fallback = TemporalPolicyDefaults.snapshot(entity);
        return TemporalPolicyResolver.getInstance()
                        .resolveEntity(entity, Operation.SNAPSHOT, fallback)
                        .decision()
                == Decision.ALLOW;
    }

    private static boolean shouldExcludePlayer(Entity entity, boolean includePlayers) {
        if (includePlayers) {
            return false;
        }
        return entity instanceof ServerPlayer;
    }

    private static boolean isCandidate(
            Entity entity, ServerPlayer owner, boolean includePlayers, double radiusSquared) {
        if (!shouldTrack(entity, includePlayers)) {
            return false;
        }
        return entity.distanceToSqr(owner) <= radiusSquared;
    }

    private static boolean shouldLogSnapshotState(
            Collection<TemporalSession> activeSessions, int currentTick) {
        if (activeSessions.isEmpty()) {
            return false;
        }
        return currentTick % 100 == 0;
    }

    public record BufferStats(int size, int capacity, int latestSnapshotTick) {}
}
