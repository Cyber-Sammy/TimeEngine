package com.time_engine.common.snapshot;

import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SnapshotManager {
    private static final SnapshotManager INSTANCE = new SnapshotManager();

    private final Map<UUID, EntitySnapshotBuffer> buffersByEntity = new HashMap<>();
    private int bufferCapacity;

    private SnapshotManager() {
    }

    public static SnapshotManager getInstance() {
        return INSTANCE;
    }

    public void tick(MinecraftServer server) {
        int currentTick = server.getTickCount();
        int configuredCapacity = TimeEngineConfig.snapshotHistoryTicks() + 1;
        if (bufferCapacity != configuredCapacity) {
            resetForCapacity(configuredCapacity);
        }

        Set<UUID> capturedEntityIds = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            capture(player, currentTick, capturedEntityIds);
        }

        Collection<TemporalSession> activeSessions = TemporalSessionManager.getInstance().getActiveSessions();
        for (TemporalSession session : activeSessions) {
            ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerPlayerId());
            if (owner != null) {
                captureNearbyEntities(owner, session.radius(), currentTick, capturedEntityIds);
            }
        }

        int oldestRetainedTick = currentTick - TimeEngineConfig.snapshotHistoryTicks();
        buffersByEntity.values().removeIf(buffer -> buffer.latestRecordedTick() < oldestRetainedTick);

        if (!activeSessions.isEmpty() && currentTick % 100 == 0) {
            ModLog.diagnostic(
                    "Snapshot state at tick {}: captured={}, trackedBuffers={}",
                    currentTick, capturedEntityIds.size(), buffersByEntity.size()
            );
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

    public int trackedEntityCount() {
        return buffersByEntity.size();
    }

    public void clear() {
        ModLog.diagnostic("Clearing {} entity snapshot buffers", buffersByEntity.size());
        buffersByEntity.clear();
        bufferCapacity = 0;
    }

    private void captureNearbyEntities(
            ServerPlayer owner,
            double radius,
            int currentTick,
            Set<UUID> capturedEntityIds
    ) {
        AABB searchBounds = owner.getBoundingBox().inflate(radius);
        double radiusSquared = radius * radius;
        List<Entity> candidates = owner.serverLevel().getEntities(
                owner,
                searchBounds,
                entity -> shouldTrack(entity) && entity.distanceToSqr(owner) <= radiusSquared
        );
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
                .computeIfAbsent(entity.getUUID(), ignored -> new EntitySnapshotBuffer(entity.getUUID(), bufferCapacity))
                .addSnapshot(EntitySnapshot.capture(entity, currentTick));
    }

    private void resetForCapacity(int configuredCapacity) {
        if (!buffersByEntity.isEmpty()) {
            ModLog.diagnostic(
                    "Snapshot history capacity changed from {} to {}; clearing existing history",
                    bufferCapacity, configuredCapacity
            );
            buffersByEntity.clear();
        }
        bufferCapacity = configuredCapacity;
    }

    private static boolean shouldTrack(Entity entity) {
        return !entity.isRemoved() && (entity instanceof Mob || entity instanceof Projectile);
    }
}
