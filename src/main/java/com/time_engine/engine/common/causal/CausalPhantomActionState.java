package com.time_engine.engine.common.causal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record CausalPhantomActionState(
        boolean swinging, boolean attacking, boolean blocking, boolean usingItem) {
    public static final CausalPhantomActionState NONE =
            new CausalPhantomActionState(false, false, false, false);

    public static CausalPhantomActionState capture(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return NONE;
        }
        return fromLivingEntity(livingEntity);
    }

    public static CausalPhantomActionState fromLivingFlags(
            boolean swinging, boolean blocking, boolean usingItem) {
        return new CausalPhantomActionState(
                swinging, isAttackSwing(swinging, blocking, usingItem), blocking, usingItem);
    }

    private static CausalPhantomActionState fromLivingEntity(LivingEntity livingEntity) {
        boolean blocking = livingEntity.isBlocking();
        boolean usingItem = livingEntity.isUsingItem();
        boolean swinging = livingEntity.swinging;
        return fromLivingFlags(swinging, blocking, usingItem);
    }

    private static boolean isAttackSwing(boolean swinging, boolean blocking, boolean usingItem) {
        if (!swinging) {
            return false;
        }
        if (blocking) {
            return false;
        }
        return !usingItem;
    }
}
