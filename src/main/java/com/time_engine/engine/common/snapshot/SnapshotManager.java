package com.time_engine.engine.common.snapshot;

import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.engine.common.policy.TemporalPolicyResolver;
import com.time_engine.engine.common.temporal.TemporalSession;
import com.time_engine.engine.common.temporal.TemporalSessionManager;
import com.time_engine.engine.config.TimeEngineConfig;
import com.time_engine.engine.util.ModLog;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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
    private static final double BOUNDARY_EXIT_CAPTURE_GRACE_BLOCKS = 2.0D;

    private final Map<UUID, EntitySnapshotBuffer> buffersByEntity = new HashMap<>();
    private final Map<UUID, SessionEntityAdmission> admissionBySession = new HashMap<>();
    private final Map<UUID, TrackingDiagnostics> trackingDiagnosticsBySession = new HashMap<>();
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
                        session,
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
        admissionBySession.keySet().removeIf(sessionId -> !isActive(sessionId, activeSessions));
        trackingDiagnosticsBySession
                .keySet()
                .removeIf(sessionId -> !isActive(sessionId, activeSessions));

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

    public Set<UUID> trackedEntityIds() {
        return Set.copyOf(buffersByEntity.keySet());
    }

    public Set<UUID> admittedEntityIds(UUID sessionId) {
        SessionEntityAdmission admission = admissionBySession.get(sessionId);
        return admission == null ? Set.of() : admission.admittedIds();
    }

    public boolean isAdmitted(UUID sessionId, UUID entityId) {
        SessionEntityAdmission admission = admissionBySession.get(sessionId);
        return admission != null && admission.contains(entityId);
    }

    public OptionalInt getAdmissionTick(UUID sessionId, UUID entityId) {
        SessionEntityAdmission admission = admissionBySession.get(sessionId);
        if (admission == null) {
            return OptionalInt.empty();
        }
        return admission.admissionTick(entityId);
    }

    public Optional<BufferStats> getBufferStats(UUID entityId) {
        EntitySnapshotBuffer buffer = buffersByEntity.get(entityId);
        if (buffer == null) {
            return Optional.empty();
        }
        return Optional.of(
                new BufferStats(buffer.size(), buffer.capacity(), buffer.latestRecordedTick()));
    }

    public Optional<TrackingDiagnostics> getTrackingDiagnostics(UUID sessionId) {
        return Optional.ofNullable(trackingDiagnosticsBySession.get(sessionId));
    }

    public void clear() {
        ModLog.diagnostic("Clearing {} entity snapshot buffers", buffersByEntity.size());
        buffersByEntity.clear();
        admissionBySession.clear();
        trackingDiagnosticsBySession.clear();
        bufferCapacity = 0;
    }

    private void captureNearbyEntities(
            ServerPlayer owner,
            TemporalSession session,
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

        SessionEntityAdmission admission =
                admissionBySession.computeIfAbsent(
                        session.sessionId(), ignored -> new SessionEntityAdmission());
        Set<UUID> retainableIds =
                retainableIds(owner, includePlayers, radius, candidates, admission.admittedIds());
        List<UUID> candidateIds = candidates.stream().map(Entity::getUUID).toList();
        SessionEntityAdmission.AdmissionUpdate update =
                admission.update(
                        candidateIds,
                        retainableIds,
                        TimeEngineConfig.trackNewEntitiesEnteringSessionRadius(),
                        TimeEngineConfig.maxTrackedEntitiesPerSession(),
                        currentTick);
        Set<UUID> admittedIds = admission.admittedIds();
        for (Entity candidate : candidates) {
            if (!admittedIds.contains(candidate.getUUID())) {
                continue;
            }
            capture(candidate, currentTick, capturedEntityIds);
        }
        captureBoundaryExitEntities(
                owner, includePlayers, currentTick, capturedEntityIds, admittedIds, radius);
        recordDiagnostics(session, admittedIds, update);
    }

    private void recordDiagnostics(
            TemporalSession session,
            Set<UUID> admittedIds,
            SessionEntityAdmission.AdmissionUpdate update) {
        trackingDiagnosticsBySession.put(
                session.sessionId(),
                new TrackingDiagnostics(
                        admittedIds.size(), update.newlyAdmitted(), update.capReached()));
        if (update.newlyAdmitted() > 0 || update.evicted() > 0 || update.capReached()) {
            ModLog.diagnostic(
                    "Dynamic tracking for session {}: admitted={}, newlyAdmitted={}, evicted={}, capReached={}",
                    session.sessionId(),
                    admittedIds.size(),
                    update.newlyAdmitted(),
                    update.evicted(),
                    update.capReached());
        }
    }

    private Set<UUID> retainableIds(
            ServerPlayer owner,
            boolean includePlayers,
            double radius,
            List<Entity> candidates,
            Set<UUID> admittedIds) {
        Set<UUID> retainableIds = new HashSet<>();
        candidates.stream().map(Entity::getUUID).forEach(retainableIds::add);

        double radiusSquared = radius * radius;
        double outerRadius = radius + BOUNDARY_EXIT_CAPTURE_GRACE_BLOCKS;
        double outerRadiusSquared = outerRadius * outerRadius;
        for (UUID entityId : admittedIds) {
            Entity entity = owner.serverLevel().getEntity(entityId);
            if (isBoundaryExitCandidate(
                    entity, owner, includePlayers, radiusSquared, outerRadiusSquared)) {
                retainableIds.add(entityId);
            }
        }
        return retainableIds;
    }

    private void captureBoundaryExitEntities(
            ServerPlayer owner,
            boolean includePlayers,
            int currentTick,
            Set<UUID> capturedEntityIds,
            Set<UUID> admittedIds,
            double radius) {
        double outerRadius = radius + BOUNDARY_EXIT_CAPTURE_GRACE_BLOCKS;
        double radiusSquared = radius * radius;
        double outerRadiusSquared = outerRadius * outerRadius;
        for (UUID entityId : admittedIds) {
            Entity entity = owner.serverLevel().getEntity(entityId);
            if (!isBoundaryExitCandidate(
                    entity, owner, includePlayers, radiusSquared, outerRadiusSquared)) {
                continue;
            }
            capture(entity, currentTick, capturedEntityIds);
        }
    }

    private static boolean isBoundaryExitCandidate(
            Entity entity,
            ServerPlayer owner,
            boolean includePlayers,
            double radiusSquared,
            double outerRadiusSquared) {
        if (entity == null) {
            return false;
        }
        if (!shouldTrack(entity, includePlayers)) {
            return false;
        }
        double distanceSquared = entity.distanceToSqr(owner);
        if (distanceSquared <= radiusSquared) {
            return false;
        }
        return distanceSquared <= outerRadiusSquared;
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
        admissionBySession.clear();
        trackingDiagnosticsBySession.clear();
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

    private static boolean isActive(UUID sessionId, Collection<TemporalSession> activeSessions) {
        return activeSessions.stream().anyMatch(session -> session.sessionId().equals(sessionId));
    }

    public record BufferStats(int size, int capacity, int latestSnapshotTick) {}

    public record TrackingDiagnostics(
            int admittedEntities, int newlyAdmittedEntities, boolean capReached) {}
}
