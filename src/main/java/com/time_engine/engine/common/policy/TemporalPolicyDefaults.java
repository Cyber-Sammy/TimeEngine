package com.time_engine.engine.common.policy;

import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;

public final class TemporalPolicyDefaults {
    private TemporalPolicyDefaults() {}

    public static Decision snapshot(Entity entity) {
        return snapshot(
                entity instanceof ServerPlayer,
                entity instanceof Mob,
                entity instanceof Projectile);
    }

    static Decision snapshot(boolean serverPlayer, boolean mob, boolean projectile) {
        if (serverPlayer) {
            return Decision.ALLOW;
        }
        if (mob) {
            return Decision.ALLOW;
        }
        return Decision.IGNORE;
    }

    public static Decision phantomCombat(Entity entity) {
        return phantomCombat(entity instanceof ServerPlayer, entity instanceof Mob);
    }

    static Decision phantomCombat(boolean serverPlayer, boolean mob) {
        if (serverPlayer) {
            return Decision.ALLOW;
        }
        if (mob) {
            return Decision.ALLOW;
        }
        return Decision.IGNORE;
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
