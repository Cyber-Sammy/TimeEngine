package com.time_engine.common.combat;

import com.time_engine.config.TimeEngineConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class TemporalDamageHandler {
    private TemporalDamageHandler() {}

    public static boolean apply(ServerPlayer attacker, Entity target) {
        float damage =
                (float)
                        (attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)
                                * TimeEngineConfig.phantomDamageMultiplier());
        if (damage <= 0.0F) {
            return false;
        }
        if (!target.hurt(attacker.damageSources().playerAttack(attacker), damage)) {
            return false;
        }

        attacker.resetAttackStrengthTicker();
        attacker.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
