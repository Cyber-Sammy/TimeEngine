package com.time_engine.common.combat;

import com.time_engine.common.snapshot.EntitySnapshot;
import com.time_engine.common.snapshot.SnapshotManager;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class TemporalAttackValidator {
    private static final TemporalAttackValidator INSTANCE = new TemporalAttackValidator();

    private final Map<UUID, Integer> cooldownEndTicks = new HashMap<>();

    private TemporalAttackValidator() {}

    public static TemporalAttackValidator getInstance() {
        return INSTANCE;
    }

    public ValidationResult validate(
            ServerPlayer attacker, UUID targetEntityId, double clientPerceivedTick) {
        TemporalSessionManager sessionManager = TemporalSessionManager.getInstance();
        Optional<TemporalSession> sessionResult = sessionManager.getSession(attacker);
        if (sessionResult.isEmpty()) {
            return ValidationResult.rejected(RejectionReason.NO_ACTIVE_SESSION);
        }

        int serverTick = attacker.getServer().getTickCount();
        if (cooldownEndTicks.getOrDefault(attacker.getUUID(), 0) > serverTick) {
            return ValidationResult.rejected(RejectionReason.ATTACK_COOLDOWN);
        }

        Entity target = attacker.serverLevel().getEntity(targetEntityId);
        if (target == null || target == attacker) {
            return ValidationResult.rejected(RejectionReason.TARGET_NOT_FOUND);
        }
        if (!target.isAlive() || !target.isAttackable()) {
            return ValidationResult.rejected(RejectionReason.TARGET_NOT_ATTACKABLE);
        }

        TemporalSession session = sessionResult.orElseThrow();
        double serverPerceivedTick = sessionManager.getPerceivedTick(session, serverTick);
        if (clientPerceivedTick < session.startTick()
                || clientPerceivedTick > session.endTick()
                || !isPerceivedTickWithinDrift(
                        clientPerceivedTick,
                        serverPerceivedTick,
                        TimeEngineConfig.phantomAllowedHitTickDrift())) {
            return ValidationResult.rejected(RejectionReason.INVALID_CLIENT_TICK);
        }
        Optional<EntitySnapshot> snapshotResult =
                SnapshotManager.getInstance()
                        .getInterpolatedSnapshot(targetEntityId, clientPerceivedTick);
        if (snapshotResult.isEmpty()) {
            return ValidationResult.rejected(RejectionReason.SNAPSHOT_NOT_FOUND);
        }

        EntitySnapshot snapshot = snapshotResult.orElseThrow();
        if (!snapshot.dimension().equals(attacker.level().dimension()) || !snapshot.alive()) {
            return ValidationResult.rejected(RejectionReason.INVALID_SNAPSHOT);
        }

        Vec3 attackOrigin = attacker.getEyePosition();
        OptionalDouble hitDistance =
                PhantomHitDetector.rayIntersectionDistance(
                        attackOrigin,
                        attacker.getLookAngle(),
                        TimeEngineConfig.phantomAttackReach(),
                        snapshot.boundingBox());
        if (hitDistance.isEmpty()) {
            return ValidationResult.rejected(RejectionReason.RAY_MISSED_HISTORICAL_BOUNDS);
        }

        return ValidationResult.accepted(
                new ValidatedAttack(
                        target,
                        serverPerceivedTick,
                        clientPerceivedTick,
                        hitDistance.getAsDouble()));
    }

    public void recordSuccessfulAttack(ServerPlayer attacker) {
        int serverTick = attacker.getServer().getTickCount();
        cooldownEndTicks.put(
                attacker.getUUID(), serverTick + TimeEngineConfig.phantomAttackCooldownTicks());
    }

    public void clearPlayer(UUID playerId) {
        cooldownEndTicks.remove(playerId);
    }

    public void clear() {
        cooldownEndTicks.clear();
    }

    static boolean isPerceivedTickWithinDrift(
            double clientPerceivedTick, double serverPerceivedTick, double allowedDrift) {
        return Double.isFinite(clientPerceivedTick)
                && Double.isFinite(serverPerceivedTick)
                && allowedDrift >= 0.0D
                && Math.abs(clientPerceivedTick - serverPerceivedTick) <= allowedDrift;
    }

    public enum RejectionReason {
        NONE,
        NO_ACTIVE_SESSION,
        ATTACK_COOLDOWN,
        TARGET_NOT_FOUND,
        TARGET_NOT_ATTACKABLE,
        SNAPSHOT_NOT_FOUND,
        INVALID_SNAPSHOT,
        RAY_MISSED_HISTORICAL_BOUNDS,
        INVALID_CLIENT_TICK,
        REQUEST_RATE_LIMITED,
        DAMAGE_REJECTED
    }

    public record ValidatedAttack(
            Entity target,
            double serverPerceivedTick,
            double validatedPerceivedTick,
            double hitDistance) {}

    public record ValidationResult(ValidatedAttack attack, RejectionReason rejectionReason) {
        public static ValidationResult accepted(ValidatedAttack attack) {
            return new ValidationResult(attack, RejectionReason.NONE);
        }

        public static ValidationResult rejected(RejectionReason reason) {
            return new ValidationResult(null, reason);
        }

        public boolean accepted() {
            return attack != null;
        }
    }
}
