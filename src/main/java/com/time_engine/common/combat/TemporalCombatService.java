package com.time_engine.common.combat;

import com.time_engine.common.combat.TemporalAttackValidator.RejectionReason;
import com.time_engine.common.combat.TemporalAttackValidator.ValidatedAttack;
import com.time_engine.common.combat.TemporalAttackValidator.ValidationResult;
import com.time_engine.common.network.PhantomHitRequestPayload;
import com.time_engine.util.ModLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalCombatService {
    private static final int REJECTION_LOG_INTERVAL_TICKS = 20;
    private static final TemporalCombatService INSTANCE = new TemporalCombatService();

    private final Map<UUID, Integer> lastRequestTicks = new HashMap<>();
    private final Map<UUID, Integer> lastRejectionLogTicks = new HashMap<>();
    private PhantomDamageResolver damageResolver = VanillaPhantomDamageResolver.INSTANCE;

    private TemporalCombatService() {}

    public static TemporalCombatService getInstance() {
        return INSTANCE;
    }

    public void handle(ServerPlayer attacker, PhantomHitRequestPayload payload) {
        int serverTick = attacker.getServer().getTickCount();
        UUID attackerId = attacker.getUUID();
        if (lastRequestTicks.getOrDefault(attackerId, Integer.MIN_VALUE) == serverTick) {
            logRejection(attacker, RejectionReason.REQUEST_RATE_LIMITED, serverTick);
            return;
        }
        lastRequestTicks.put(attackerId, serverTick);

        ValidationResult result =
                TemporalAttackValidator.getInstance()
                        .validate(
                                attacker, payload.targetEntityId(), payload.clientPerceivedTick());
        if (!result.accepted()) {
            logRejection(attacker, result.rejectionReason(), serverTick);
            return;
        }

        ValidatedAttack attack = result.attack();
        PhantomDamageResult damageResult =
                damageResolver.apply(PhantomDamageContext.from(attacker, attack));
        if (!damageResult.applied()) {
            ModLog.diagnostic(
                    "Rejected phantom damage from {} to {}: {}",
                    attackerId,
                    attack.target().getUUID(),
                    damageResult.reason());
            logRejection(attacker, RejectionReason.DAMAGE_REJECTED, serverTick);
            return;
        }
        TemporalAttackValidator.getInstance().recordSuccessfulAttack(attacker);

        ModLog.diagnostic(
                "Accepted phantom hit from {} to {} at server tick {} (serverPerceivedTick={}, clientPerceivedTick={}, hitDistance={})",
                attackerId,
                attack.target().getUUID(),
                serverTick,
                attack.serverPerceivedTick(),
                attack.validatedPerceivedTick(),
                attack.hitDistance());
    }

    void setDamageResolverForTests(PhantomDamageResolver damageResolver) {
        this.damageResolver = damageResolver;
    }

    void resetDamageResolverForTests() {
        damageResolver = VanillaPhantomDamageResolver.INSTANCE;
    }

    public void clearPlayer(UUID playerId) {
        lastRequestTicks.remove(playerId);
        lastRejectionLogTicks.remove(playerId);
        TemporalAttackValidator.getInstance().clearPlayer(playerId);
    }

    public void clear() {
        lastRequestTicks.clear();
        lastRejectionLogTicks.clear();
        TemporalAttackValidator.getInstance().clear();
    }

    private void logRejection(ServerPlayer attacker, RejectionReason reason, int serverTick) {
        UUID attackerId = attacker.getUUID();
        Integer lastLogTick = lastRejectionLogTicks.get(attackerId);
        if (shouldSuppressRejectionLog(lastLogTick, serverTick)) {
            return;
        }

        lastRejectionLogTicks.put(attackerId, serverTick);
        ModLog.diagnostic(
                "Rejected phantom hit from player {} at tick {}: {}",
                attackerId,
                serverTick,
                reason);
    }

    private static boolean shouldSuppressRejectionLog(Integer lastLogTick, int serverTick) {
        if (lastLogTick == null) {
            return false;
        }
        return serverTick - lastLogTick < REJECTION_LOG_INTERVAL_TICKS;
    }
}
