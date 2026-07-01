package com.time_engine.common.network;

import com.time_engine.common.intercept.TemporalInterceptManager;
import com.time_engine.common.network.GhostFramePayload.GhostFrameEntity;
import com.time_engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.common.policy.TemporalPolicyResolver;
import com.time_engine.common.snapshot.EntitySnapshot;
import com.time_engine.common.snapshot.SnapshotManager;
import com.time_engine.common.temporal.TemporalLayerRelation;
import com.time_engine.common.temporal.TemporalScaleResolver;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GhostFrameBroadcaster {
    private GhostFrameBroadcaster() {}

    public static void tick(MinecraftServer server) {
        int serverTick = server.getTickCount();
        if (serverTick % TimeEngineConfig.ghostFrameIntervalTicks() != 0) {
            return;
        }

        TemporalSessionManager sessionManager = TemporalSessionManager.getInstance();
        SnapshotManager snapshotManager = SnapshotManager.getInstance();
        TemporalScaleResolver scaleResolver = TemporalScaleResolver.server();
        for (TemporalSession session : sessionManager.getActiveSessions()) {
            sendGhostFrame(
                    server, serverTick, session, sessionManager, snapshotManager, scaleResolver);
        }
    }

    private static void sendGhostFrame(
            MinecraftServer server,
            int serverTick,
            TemporalSession session,
            TemporalSessionManager sessionManager,
            SnapshotManager snapshotManager,
            TemporalScaleResolver scaleResolver) {
        ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerPlayerId());
        if (owner == null) {
            return;
        }

        List<GhostFrameEntity> ghostEntities =
                collectGhostEntities(owner, session, serverTick, snapshotManager, scaleResolver);
        double framePerceivedTick = sessionManager.getPerceivedTick(session, serverTick);
        PacketDistributor.sendToPlayer(
                owner,
                GhostFramePayload.create(
                        session.sessionId(),
                        serverTick,
                        framePerceivedTick,
                        owner.level().dimension().location(),
                        ghostEntities));
        logFrameIfNeeded(session, serverTick, framePerceivedTick, ghostEntities.size());
    }

    private static List<GhostFrameEntity> collectGhostEntities(
            ServerPlayer owner,
            TemporalSession session,
            int serverTick,
            SnapshotManager snapshotManager,
            TemporalScaleResolver scaleResolver) {
        AABB searchBounds = owner.getBoundingBox().inflate(session.radius());
        double radiusSquared = session.radius() * session.radius();
        Map<UUID, Entity> candidatesById = new LinkedHashMap<>();
        owner
                .serverLevel()
                .getEntities(
                        owner,
                        searchBounds,
                        entity -> isCurrentCandidate(entity, owner, radiusSquared))
                .stream()
                .filter(entity -> isAdmittedOrAlwaysTrackedPlayer(session, snapshotManager, entity))
                .forEach(entity -> candidatesById.put(entity.getUUID(), entity));
        snapshotManager
                .admittedEntityIds(session.sessionId())
                .forEach(
                        entityId -> {
                            Entity entity = owner.serverLevel().getEntity(entityId);
                            if (entity != null && !entity.getUUID().equals(owner.getUUID())) {
                                candidatesById.putIfAbsent(entityId, entity);
                            }
                        });

        return candidatesById.values().stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(owner)))
                .map(
                        entity ->
                                createGhostEntity(
                                        owner,
                                        entity,
                                        session,
                                        serverTick,
                                        snapshotManager,
                                        scaleResolver))
                .flatMap(Optional::stream)
                .limit(TimeEngineConfig.maxTrackedEntitiesPerSession())
                .toList();
    }

    private static Optional<GhostFrameEntity> createGhostEntity(
            ServerPlayer owner,
            Entity target,
            TemporalSession session,
            int serverTick,
            SnapshotManager snapshotManager,
            TemporalScaleResolver scaleResolver) {
        TemporalLayerRelation relation = relation(owner, target, scaleResolver);
        if (!relation.allowsAttackableGhost()) {
            return Optional.empty();
        }

        double perceivedTick =
                effectivePerceivedTick(
                        snapshotManager,
                        session,
                        target,
                        scaleResolver.relativePerceivedTick(session, target, serverTick));
        return snapshotForGhost(session, target, perceivedTick, snapshotManager)
                .filter(snapshot -> isUsableSnapshot(snapshot, owner))
                .filter(snapshot -> canRenderWithinBoundary(snapshot, owner, session))
                .map(
                        snapshot ->
                                new GhostFrameEntity(
                                        GhostFrameBoundary.clampToRadius(
                                                snapshot, owner.position(), session.radius()),
                                        perceivedTick,
                                        isPhantomCombatAllowed(target)));
    }

    private static Optional<EntitySnapshot> snapshotForGhost(
            TemporalSession session,
            Entity target,
            double perceivedTick,
            SnapshotManager snapshotManager) {
        Optional<EntitySnapshot> splicedSnapshot =
                TemporalInterceptManager.getInstance()
                        .getSplicedSnapshot(session.sessionId(), target.getUUID(), perceivedTick);
        if (splicedSnapshot.isPresent()) {
            return splicedSnapshot;
        }
        return snapshotManager.getInterpolatedSnapshot(target.getUUID(), perceivedTick);
    }

    private static TemporalLayerRelation relation(
            ServerPlayer owner, Entity target, TemporalScaleResolver scaleResolver) {
        return TemporalLayerRelation.compare(
                scaleResolver.effectiveScale(owner), scaleResolver.effectiveScale(target));
    }

    private static boolean isCurrentCandidate(
            Entity entity, ServerPlayer owner, double radiusSquared) {
        if (entity.isRemoved()) {
            return false;
        }
        if (entity.getUUID().equals(owner.getUUID())) {
            return false;
        }
        return entity.distanceToSqr(owner) <= radiusSquared;
    }

    private static boolean isAdmittedOrAlwaysTrackedPlayer(
            TemporalSession session, SnapshotManager snapshotManager, Entity entity) {
        if (entity instanceof ServerPlayer && TimeEngineConfig.snapshotPlayersAlways()) {
            return true;
        }
        return snapshotManager.isAdmitted(session.sessionId(), entity.getUUID());
    }

    private static double effectivePerceivedTick(
            SnapshotManager snapshotManager,
            TemporalSession session,
            Entity target,
            double rawPerceivedTick) {
        OptionalInt admissionTick =
                snapshotManager.getAdmissionTick(session.sessionId(), target.getUUID());
        if (admissionTick.isEmpty()) {
            return rawPerceivedTick;
        }
        return Math.max(rawPerceivedTick, admissionTick.getAsInt());
    }

    private static boolean isUsableSnapshot(EntitySnapshot snapshot, ServerPlayer owner) {
        if (!snapshot.alive()) {
            return false;
        }
        return snapshot.dimension().equals(owner.level().dimension());
    }

    private static boolean canRenderWithinBoundary(
            EntitySnapshot snapshot, ServerPlayer owner, TemporalSession session) {
        return GhostFrameBoundary.canRenderAtBoundary(snapshot, owner.position(), session.radius());
    }

    private static boolean isPhantomCombatAllowed(Entity target) {
        return TemporalPolicyResolver.getInstance()
                        .resolveEntity(
                                target,
                                Operation.PHANTOM_COMBAT,
                                TemporalPolicyDefaults.phantomCombat(target))
                        .decision()
                == Decision.ALLOW;
    }

    private static void logFrameIfNeeded(
            TemporalSession session, int serverTick, double perceivedTick, int entityCount) {
        if (serverTick % 100 != 0) {
            return;
        }
        ModLog.diagnostic(
                "Sent ghost frame for session {} at server tick {} (perceivedTick={}, entities={})",
                session.sessionId(),
                serverTick,
                perceivedTick,
                entityCount);
    }
}
