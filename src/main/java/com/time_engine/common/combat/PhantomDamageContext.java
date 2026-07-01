package com.time_engine.common.combat;

import com.time_engine.common.combat.TemporalAttackValidator.ValidatedAttack;
import com.time_engine.config.TimeEngineConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record PhantomDamageContext(
        ServerPlayer attacker,
        Entity target,
        ValidatedAttack validatedAttack,
        float damageMultiplier) {
    public static PhantomDamageContext from(ServerPlayer attacker, ValidatedAttack attack) {
        return new PhantomDamageContext(
                attacker, attack.target(), attack, TimeEngineConfig.phantomDamageMultiplier());
    }
}
