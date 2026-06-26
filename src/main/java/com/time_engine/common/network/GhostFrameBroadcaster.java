package com.time_engine.common.network;

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
import java.util.List;
import java.util.Optional;
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
        return owner
                .serverLevel()
                .getEntities(
                        owner,
                        searchBounds,
                        entity -> isCurrentCandidate(entity, owner, radiusSquared))
                .stream()
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

        double perceivedTick = scaleResolver.relativePerceivedTick(session, target, serverTick);
        return snapshotManager
                .getInterpolatedSnapshot(target.getUUID(), perceivedTick)
                .filter(snapshot -> isUsableSnapshot(snapshot, owner))
                .map(
                        snapshot ->
                                new GhostFrameEntity(
                                        snapshot, perceivedTick, isPhantomCombatAllowed(target)));
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

    private static boolean isUsableSnapshot(EntitySnapshot snapshot, ServerPlayer owner) {
        if (!snapshot.alive()) {
            return false;
        }
        return snapshot.dimension().equals(owner.level().dimension());
    }

    private static boolean isPhantomCombatAllowed(Entity target) {
        return TemporalPolicyResolver.getInstance()
                        .resolveEntity(
                                target,
                                Operation.PHANTOM_COMBAT,
                                TemporalPolicyDefaults.phantomCombat())
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
