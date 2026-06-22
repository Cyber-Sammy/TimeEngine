package com.time_engine.common.intercept;

import com.time_engine.common.intercept.TemporalInterceptSessionState.TrackedBlock;
import com.time_engine.common.snapshot.EntitySnapshot;
import com.time_engine.common.snapshot.SnapshotManager;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class TemporalInterceptResolver {
    private TemporalInterceptResolver() {}

    static void evaluate(
            ServerPlayer owner,
            TemporalSession session,
            TemporalInterceptSessionState state,
            double previousPerceivedTick,
            double currentPerceivedTick) {
        SnapshotManager snapshotManager = SnapshotManager.getInstance();
        List<EntitySnapshot> currentSnapshots =
                snapshotManager.getInterpolatedSnapshots(
                        owner.level().dimension(),
                        owner.position(),
                        session.radius(),
                        currentPerceivedTick,
                        owner.getUUID(),
                        TimeEngineConfig.maxTrackedEntitiesPerSession());

        for (EntitySnapshot currentSnapshot : currentSnapshots) {
            evaluateSnapshot(
                    owner, session, state, snapshotManager, currentSnapshot, previousPerceivedTick);
        }
    }

    private static void evaluateSnapshot(
            ServerPlayer owner,
            TemporalSession session,
            TemporalInterceptSessionState state,
            SnapshotManager snapshotManager,
            EntitySnapshot currentSnapshot,
            double previousPerceivedTick) {
        Entity target = owner.serverLevel().getEntity(currentSnapshot.entityId());
        if (!isSupportedTarget(owner, target)) {
            return;
        }
        Optional<EntitySnapshot> previousSnapshot =
                snapshotManager.getInterpolatedSnapshot(
                        currentSnapshot.entityId(), previousPerceivedTick);
        if (previousSnapshot.isEmpty()) {
            return;
        }
        evaluateTargetPath(
                owner,
                session,
                state,
                snapshotManager,
                target,
                previousSnapshot.orElseThrow(),
                currentSnapshot,
                previousPerceivedTick);
    }

    private static void evaluateTargetPath(
            ServerPlayer owner,
            TemporalSession session,
            TemporalInterceptSessionState state,
            SnapshotManager snapshotManager,
            Entity target,
            EntitySnapshot previousSnapshot,
            EntitySnapshot currentSnapshot,
            double previousPerceivedTick) {
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
                        tick -> snapshotManager.getInterpolatedSnapshot(target.getUUID(), tick));
        if (safeSnapshot.isEmpty()) {
            ModLog.diagnostic(
                    "Rejected temporal intercept for entity {} at block {}: no safe historical snapshot",
                    target.getUUID(),
                    intercept.block().record().position());
            return;
        }
        if (!applyCorrection(owner.serverLevel(), target, safeSnapshot.orElseThrow())) {
            return;
        }

        intercept.block().markIntercepted(target.getUUID());
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

    private static OptionalDouble findCollision(
            TrackedBlock block,
            Entity target,
            EntitySnapshot previousSnapshot,
            EntitySnapshot currentSnapshot) {
        if (block.hasIntercepted(target.getUUID())) {
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

        target.teleportTo(
                safeSnapshot.position().x, safeSnapshot.position().y, safeSnapshot.position().z);
        target.setYRot(safeSnapshot.yRot());
        target.setXRot(safeSnapshot.xRot());
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        return true;
    }

    private static boolean isSupportedTarget(ServerPlayer owner, Entity target) {
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
        if (target instanceof ServerPlayer) {
            return true;
        }
        return target instanceof Mob;
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
