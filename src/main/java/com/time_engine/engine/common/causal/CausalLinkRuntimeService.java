package com.time_engine.engine.common.causal;

import com.time_engine.engine.common.intercept.TemporalInterceptManager;
import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.engine.common.policy.TemporalPolicyResolver;
import com.time_engine.engine.common.snapshot.EntitySnapshot;
import com.time_engine.engine.common.snapshot.SnapshotManager;
import com.time_engine.engine.common.temporal.TemporalLayerRelation;
import com.time_engine.engine.common.temporal.TemporalScaleResolver;
import com.time_engine.engine.common.temporal.TemporalSession;
import com.time_engine.engine.common.temporal.TemporalSessionManager;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CausalLinkRuntimeService {
    private static final CausalLinkRuntimeService INSTANCE = new CausalLinkRuntimeService();
    private static final double LOCKED_RADIUS_MULTIPLIER = 2.0D;
    private static final double MAX_DISTANCE_MULTIPLIER = 3.0D;

    private final CausalLinkManager linkManager = new CausalLinkManager();
    private final CausalTargetSelector targetSelector =
            new CausalTargetSelector(CausalTargetSelectionRules.DEFAULT);

    private CausalLinkRuntimeService() {}

    public static CausalLinkRuntimeService getInstance() {
        return INSTANCE;
    }

    public CausalLinkManager linkManager() {
        return linkManager;
    }

    public void tick(MinecraftServer server) {
        int serverTick = server.getTickCount();
        Collection<TemporalSession> activeSessions =
                TemporalSessionManager.getInstance().getActiveSessions();
        clearInactiveLinks(activeSessions);
        for (TemporalSession session : activeSessions) {
            tickSession(server, session, serverTick);
        }
    }

    public void clear() {
        linkManager.clear();
    }

    private void clearInactiveLinks(Collection<TemporalSession> activeSessions) {
        Set<UUID> activeOwnerIds =
                activeSessions.stream()
                        .map(TemporalSession::ownerPlayerId)
                        .collect(Collectors.toSet());
        linkManager.clearInactiveOwners(activeOwnerIds);
    }

    private void tickSession(MinecraftServer server, TemporalSession session, int serverTick) {
        ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerPlayerId());
        if (owner == null) {
            linkManager.clearLink(session.ownerPlayerId());
            return;
        }

        CausalTuning tuning = tuningFor(session);
        Optional<CausalLink> previousLink = linkManager.getLink(owner.getUUID());
        Collection<CausalRuntimeCandidate> candidates =
                collectCandidates(owner, session, serverTick, tuning.trackingPolicy());
        CausalTargetSelection selection =
                targetSelector.select(
                        owner.getUUID(),
                        owner.position(),
                        owner.getLookAngle(),
                        owner.getDeltaMovement(),
                        tuning.trackingPolicy(),
                        previousLink,
                        candidates.stream().map(CausalRuntimeCandidate::candidate).toList(),
                        serverTick);
        updateLink(owner, previousLink, candidates, selection, serverTick);
    }

    private CausalTuning tuningFor(TemporalSession session) {
        double lockedRadius = session.radius() * LOCKED_RADIUS_MULTIPLIER;
        double maxDistance = session.radius() * MAX_DISTANCE_MULTIPLIER;
        return CausalTuning.of(session.radius(), lockedRadius, maxDistance);
    }

    private Collection<CausalRuntimeCandidate> collectCandidates(
            ServerPlayer owner,
            TemporalSession session,
            int serverTick,
            CausalTrackingPolicy trackingPolicy) {
        Map<UUID, Entity> candidatesById = currentSessionCandidates(owner, trackingPolicy);
        addPreviousTarget(owner, candidatesById);
        return candidatesById.values().stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(owner)))
                .map(entity -> createCandidate(owner, entity, session, serverTick))
                .flatMap(Optional::stream)
                .toList();
    }

    private Map<UUID, Entity> currentSessionCandidates(
            ServerPlayer owner, CausalTrackingPolicy trackingPolicy) {
        AABB bounds = owner.getBoundingBox().inflate(trackingPolicy.sessionRadius());
        double radiusSquared = trackingPolicy.sessionRadius() * trackingPolicy.sessionRadius();
        Map<UUID, Entity> candidatesById = new LinkedHashMap<>();
        owner.serverLevel()
                .getEntities(
                        owner,
                        bounds,
                        entity -> isInsideSessionRadius(owner, entity, radiusSquared))
                .forEach(entity -> candidatesById.put(entity.getUUID(), entity));
        return candidatesById;
    }

    private void addPreviousTarget(ServerPlayer owner, Map<UUID, Entity> candidatesById) {
        linkManager
                .getLink(owner.getUUID())
                .filter(CausalLink::active)
                .map(CausalLink::targetId)
                .map(owner.serverLevel()::getEntity)
                .ifPresent(entity -> candidatesById.putIfAbsent(entity.getUUID(), entity));
    }

    private Optional<CausalRuntimeCandidate> createCandidate(
            ServerPlayer owner, Entity target, TemporalSession session, int serverTick) {
        TemporalLayerRelation relation = relation(owner, target);
        boolean allowedByPolicy = isPhantomCombatAllowed(target);
        double perceivedTick =
                perceivedTick(owner, target, session, serverTick, TemporalScaleResolver.server());
        return snapshotFor(session, target, perceivedTick)
                .filter(snapshot -> isUsableSnapshot(snapshot, owner))
                .map(
                        snapshot ->
                                CausalRuntimeCandidate.from(
                                        snapshot,
                                        relation.allowsAttackableGhost() && allowedByPolicy));
    }

    private double perceivedTick(
            ServerPlayer owner,
            Entity target,
            TemporalSession session,
            int serverTick,
            TemporalScaleResolver scaleResolver) {
        double rawPerceivedTick = scaleResolver.relativePerceivedTick(session, target, serverTick);
        OptionalInt admissionTick =
                SnapshotManager.getInstance()
                        .getAdmissionTick(session.sessionId(), target.getUUID());
        if (admissionTick.isEmpty()) {
            return rawPerceivedTick;
        }
        return Math.max(rawPerceivedTick, admissionTick.getAsInt());
    }

    private Optional<EntitySnapshot> snapshotFor(
            TemporalSession session, Entity target, double perceivedTick) {
        Optional<EntitySnapshot> splicedSnapshot =
                TemporalInterceptManager.getInstance()
                        .getSplicedSnapshot(session.sessionId(), target.getUUID(), perceivedTick);
        if (splicedSnapshot.isPresent()) {
            return splicedSnapshot;
        }
        return SnapshotManager.getInstance()
                .getInterpolatedSnapshot(target.getUUID(), perceivedTick);
    }

    private void updateLink(
            ServerPlayer owner,
            Optional<CausalLink> previousLink,
            Collection<CausalRuntimeCandidate> candidates,
            CausalTargetSelection selection,
            int serverTick) {
        Optional<CausalRuntimeCandidate> selectedCandidate =
                selectedRuntimeCandidate(candidates, selection);
        if (selectedCandidate.isEmpty()) {
            breakPreviousLink(owner, previousLink, serverTick);
            return;
        }

        CausalRuntimeCandidate runtimeCandidate = selectedCandidate.get();
        CausalPhantomFrame ownerFrame = CausalPhantomFrame.capture(owner, serverTick);
        double progress = progress(previousLink, owner.position(), runtimeCandidate.frame());
        linkManager.updateSoftLock(
                owner.getUUID(),
                selection,
                serverTick,
                runtimeCandidate.frame(),
                ownerFrame,
                progress);
    }

    private Optional<CausalRuntimeCandidate> selectedRuntimeCandidate(
            Collection<CausalRuntimeCandidate> candidates, CausalTargetSelection selection) {
        return selection
                .selectedCandidate()
                .flatMap(
                        selected ->
                                candidates.stream()
                                        .filter(candidate -> candidate.matches(selected.targetId()))
                                        .findFirst());
    }

    private void breakPreviousLink(
            ServerPlayer owner, Optional<CausalLink> previousLink, int serverTick) {
        if (previousLink.isEmpty()) {
            return;
        }
        if (!previousLink.get().active()) {
            return;
        }
        linkManager.breakLink(owner.getUUID(), serverTick);
    }

    private double progress(
            Optional<CausalLink> previousLink, Vec3 ownerPosition, CausalPhantomFrame targetFrame) {
        return previousLink
                .filter(CausalLink::active)
                .map(CausalLink::originOwnerFrame)
                .map(
                        ownerFrame ->
                                CausalProgressCalculator.progress(
                                        ownerFrame.stableAnchor(),
                                        ownerPosition,
                                        targetFrame.stableAnchor()))
                .orElse(0.0D);
    }

    private static TemporalLayerRelation relation(ServerPlayer owner, Entity target) {
        TemporalScaleResolver scaleResolver = TemporalScaleResolver.server();
        return TemporalLayerRelation.compare(
                scaleResolver.effectiveScale(owner), scaleResolver.effectiveScale(target));
    }

    private static boolean isInsideSessionRadius(
            ServerPlayer owner, Entity entity, double radiusSquared) {
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
                                TemporalPolicyDefaults.phantomCombat(target))
                        .decision()
                == Decision.ALLOW;
    }

    private record CausalRuntimeCandidate(
            CausalTargetCandidate candidate, CausalPhantomFrame frame) {
        static CausalRuntimeCandidate from(
                EntitySnapshot snapshot, boolean temporalAdvantageAllowed) {
            return new CausalRuntimeCandidate(
                    new CausalTargetCandidate(
                            snapshot.entityId(),
                            snapshot.position(),
                            snapshot.boundingBox(),
                            false,
                            temporalAdvantageAllowed),
                    CausalPhantomFrame.fromSnapshot(snapshot));
        }

        boolean matches(UUID targetId) {
            return candidate.targetId().equals(targetId);
        }
    }
}
