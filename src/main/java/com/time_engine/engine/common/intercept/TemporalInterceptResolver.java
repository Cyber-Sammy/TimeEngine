package com.time_engine.engine.common.intercept;

import com.time_engine.engine.common.intercept.TemporalInterceptSessionState.TrackedBlock;
import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.engine.common.policy.TemporalPolicyResolver;
import com.time_engine.engine.common.snapshot.EntitySnapshot;
import com.time_engine.engine.common.snapshot.SnapshotManager;
import com.time_engine.engine.common.temporal.TemporalScaleResolver;
import com.time_engine.engine.common.temporal.TemporalSession;
import com.time_engine.engine.config.TimeEngineConfig;
import com.time_engine.engine.util.ModLog;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class TemporalInterceptResolver {
    private TemporalInterceptResolver() {}

    static void evaluate(
            ServerPlayer owner,
            TemporalSession session,
            TemporalInterceptSessionState state,
            int previousServerTick,
            int currentServerTick) {
        SnapshotManager snapshotManager = SnapshotManager.getInstance();
        TemporalScaleResolver scaleResolver = TemporalScaleResolver.server();
        List<Entity> candidates = collectCandidates(owner, session.radius());

        for (Entity target : candidates) {
            evaluateSnapshot(
                    owner,
                    session,
                    state,
                    snapshotManager,
                    scaleResolver,
                    target,
                    previousServerTick,
                    currentServerTick);
        }
    }

    private static void evaluateSnapshot(
            ServerPlayer owner,
            TemporalSession session,
            TemporalInterceptSessionState state,
            SnapshotManager snapshotManager,
            TemporalScaleResolver scaleResolver,
            Entity target,
            int previousServerTick,
            int currentServerTick) {
        if (!isSupportedTarget(owner, target, scaleResolver)) {
            return;
        }

        double previousTargetTick =
                scaleResolver.relativePerceivedTick(session, target, previousServerTick);
        double currentTargetTick =
                scaleResolver.relativePerceivedTick(session, target, currentServerTick);
        if (currentTargetTick <= previousTargetTick) {
            return;
        }

        Optional<EntitySnapshot> currentSnapshot =
                snapshotForIntercept(state, target.getUUID(), currentTargetTick, snapshotManager);
        if (currentSnapshot.isEmpty()) {
            return;
        }

        Optional<EntitySnapshot> previousSnapshot =
                snapshotForIntercept(state, target.getUUID(), previousTargetTick, snapshotManager);
        if (previousSnapshot.isEmpty()) {
            return;
        }
        if (!hasContinuousDimension(
                previousSnapshot.orElseThrow(), currentSnapshot.orElseThrow())) {
            return;
        }
        evaluateTargetPath(
                owner,
                session,
                state,
                snapshotManager,
                target,
                previousSnapshot.orElseThrow(),
                currentSnapshot.orElseThrow(),
                previousTargetTick,
                currentTargetTick,
                currentServerTick);
    }

    private static void evaluateTargetPath(
            ServerPlayer owner,
            TemporalSession session,
            TemporalInterceptSessionState state,
            SnapshotManager snapshotManager,
            Entity target,
            EntitySnapshot previousSnapshot,
            EntitySnapshot currentSnapshot,
            double previousPerceivedTick,
            double currentPerceivedTick,
            int currentServerTick) {
        Optional<InterceptCandidate> candidate =
                findFirstIntercept(state, target, previousSnapshot, currentSnapshot);
        if (candidate.isEmpty()) {
            return;
        }

        InterceptCandidate intercept = candidate.orElseThrow();
        Optional<EntitySnapshot> safeSnapshot =
                SafeSnapshotResolver.resolve(
                        previousSnapshot,
                        currentSnapshot,
                        intercept.pathProgress(),
                        previousPerceivedTick,
                        session.startTick(),
                        intercept.block().record().collisionBoxes(),
                        tick ->
                                snapshotForIntercept(
                                        state, target.getUUID(), tick, snapshotManager));
        if (safeSnapshot.isEmpty()) {
            ModLog.diagnostic(
                    "Rejected temporal intercept for entity {} at block {}: no safe historical snapshot",
                    target.getUUID(),
                    intercept.block().record().position());
            return;
        }
        EntitySnapshot collapseSnapshot = safeSnapshot.orElseThrow();
        if (!applyCorrection(owner.serverLevel(), target, collapseSnapshot)) {
            return;
        }

        intercept.block().markIntercepted(target.getUUID());
        state.recordSplice(
                target.getUUID(),
                collapseTick(previousPerceivedTick, currentPerceivedTick, intercept.pathProgress()),
                currentServerTick,
                collapseSnapshot);
        showFeedback(owner.serverLevel(), target.position());
        ModLog.diagnostic(
                "Temporal intercept corrected entity {} using block {} from session {}",
                target.getUUID(),
                intercept.block().record().position(),
                intercept.block().record().sessionId());
    }

    private static Optional<InterceptCandidate> findFirstIntercept(
            TemporalInterceptSessionState state,
            Entity target,
            EntitySnapshot previousSnapshot,
            EntitySnapshot currentSnapshot) {
        InterceptCandidate earliest = null;
        for (TrackedBlock block : state.blocks()) {
            OptionalDouble collision =
                    findCollision(block, target, previousSnapshot, currentSnapshot);
            if (collision.isEmpty()) {
                continue;
            }
            if (earliest == null) {
                earliest = new InterceptCandidate(block, collision.getAsDouble());
                continue;
            }
            if (collision.getAsDouble() < earliest.pathProgress()) {
                earliest = new InterceptCandidate(block, collision.getAsDouble());
            }
        }
        return Optional.ofNullable(earliest);
    }

    private static Optional<EntitySnapshot> snapshotForIntercept(
            TemporalInterceptSessionState state,
            UUID targetId,
            double perceivedTick,
            SnapshotManager snapshotManager) {
        Optional<TemporalInterceptSessionState.TimelineSplice> splice =
                state.splice(targetId, perceivedTick);
        if (splice.isEmpty()) {
            return snapshotManager.getInterpolatedSnapshot(targetId, perceivedTick);
        }

        TemporalInterceptSessionState.TimelineSplice timelineSplice = splice.orElseThrow();
        return snapshotManager
                .getInterpolatedSnapshot(targetId, timelineSplice.mappedServerTick(perceivedTick))
                .or(() -> Optional.of(timelineSplice.fallbackSnapshot()));
    }

    private static OptionalDouble findCollision(
            TrackedBlock block,
            Entity target,
            EntitySnapshot previousSnapshot,
            EntitySnapshot currentSnapshot) {
        if (block.hasIntercepted(target.getUUID())) {
            return OptionalDouble.empty();
        }
        if (!isBlockAllowed(block.record())) {
            return OptionalDouble.empty();
        }
        return PathCollisionChecker.findFirstCollision(
                previousSnapshot.boundingBox(),
                currentSnapshot.boundingBox(),
                block.record().collisionBoxes());
    }

    private static boolean applyCorrection(
            ServerLevel level, Entity target, EntitySnapshot safeSnapshot) {
        if (!safeSnapshot.alive()) {
            return false;
        }
        double maxDistance = TimeEngineConfig.maxInterceptCorrectionDistance();
        double maxDistanceSquared = maxDistance * maxDistance;
        if (target.position().distanceToSqr(safeSnapshot.position()) > maxDistanceSquared) {
            return false;
        }
        Vec3 correctionOffset = safeSnapshot.position().subtract(target.position());
        AABB correctionBounds = target.getBoundingBox().move(correctionOffset);
        if (!level.noCollision(target, correctionBounds)) {
            return false;
        }

        teleport(level, target, safeSnapshot);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        return true;
    }

    private static void teleport(ServerLevel level, Entity target, EntitySnapshot safeSnapshot) {
        Vec3 position = safeSnapshot.position();
        if (target instanceof ServerPlayer player) {
            player.teleportTo(
                    level,
                    position.x,
                    position.y,
                    position.z,
                    safeSnapshot.yRot(),
                    safeSnapshot.xRot());
            return;
        }
        target.teleportTo(position.x, position.y, position.z);
        target.setYRot(safeSnapshot.yRot());
        target.setXRot(safeSnapshot.xRot());
    }

    private static boolean hasContinuousDimension(
            EntitySnapshot previousSnapshot, EntitySnapshot currentSnapshot) {
        return previousSnapshot.dimension().equals(currentSnapshot.dimension());
    }

    private static List<Entity> collectCandidates(ServerPlayer owner, double radius) {
        AABB searchBounds = owner.getBoundingBox().inflate(radius);
        double radiusSquared = radius * radius;
        return owner
                .serverLevel()
                .getEntities(
                        owner, searchBounds, entity -> entity.distanceToSqr(owner) <= radiusSquared)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(owner)))
                .limit(TimeEngineConfig.maxTrackedEntitiesPerSession())
                .toList();
    }

    private static boolean isSupportedTarget(
            ServerPlayer owner, Entity target, TemporalScaleResolver scaleResolver) {
        if (target == null) {
            return false;
        }
        if (target == owner) {
            return false;
        }
        if (!target.isAlive()) {
            return false;
        }
        if (target.isPassenger()) {
            return false;
        }
        if (target.isVehicle()) {
            return false;
        }
        if (!hasTemporalAdvantage(owner, target, scaleResolver)) {
            return false;
        }
        Decision fallback = TemporalPolicyDefaults.interceptEntity(target);
        return TemporalPolicyResolver.getInstance()
                        .resolveEntity(target, Operation.TEMPORAL_INTERCEPT, fallback)
                        .decision()
                == Decision.ALLOW;
    }

    private static boolean hasTemporalAdvantage(
            ServerPlayer owner, Entity target, TemporalScaleResolver scaleResolver) {
        double ownerScale = scaleResolver.effectiveScale(owner);
        double targetScale = scaleResolver.effectiveScale(target);
        return TemporalInterceptRules.allowsIntercept(ownerScale, targetScale);
    }

    private static boolean isBlockAllowed(PlacedBlockRecord block) {
        return TemporalPolicyResolver.getInstance()
                        .resolveBlock(
                                block.blockState(),
                                Operation.TEMPORAL_INTERCEPT,
                                TemporalPolicyDefaults.interceptBlock())
                        .decision()
                == Decision.ALLOW;
    }

    private static double collapseTick(
            double previousPerceivedTick, double currentPerceivedTick, double pathProgress) {
        return previousPerceivedTick
                + (currentPerceivedTick - previousPerceivedTick) * pathProgress;
    }

    private static void showFeedback(ServerLevel level, Vec3 position) {
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                position.x,
                position.y + 0.5D,
                position.z,
                24,
                0.35D,
                0.5D,
                0.35D,
                0.05D);
        level.playSound(
                null,
                position.x,
                position.y,
                position.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.7F,
                1.35F);
    }

    private record InterceptCandidate(TrackedBlock block, double pathProgress) {}
}
