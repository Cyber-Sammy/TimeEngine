package com.time_engine.common.policy;

import com.time_engine.common.policy.TemporalPolicy.Decision;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;

public final class TemporalPolicyDefaults {
    private TemporalPolicyDefaults() {}

    public static Decision snapshot(Entity entity) {
        if (entity instanceof Mob) {
            return Decision.ALLOW;
        }
        if (entity instanceof Projectile) {
            return Decision.ALLOW;
        }
        if (entity instanceof ServerPlayer) {
            return Decision.ALLOW;
        }
        return Decision.IGNORE;
    }

    public static Decision phantomCombat() {
        return Decision.ALLOW;
    }

    public static Decision interceptEntity(Entity entity) {
        if (entity instanceof ServerPlayer) {
            return Decision.ALLOW;
        }
        if (entity instanceof Mob) {
            return Decision.ALLOW;
        }
        return Decision.IGNORE;
    }

    public static Decision interceptBlock() {
        return Decision.ALLOW;
    }

    public static Decision interaction() {
        return Decision.ALLOW;
    }
}
