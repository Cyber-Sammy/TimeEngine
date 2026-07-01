package com.time_engine.common.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class VanillaPhantomDamageResolver implements PhantomDamageResolver {
    public static final VanillaPhantomDamageResolver INSTANCE = new VanillaPhantomDamageResolver();

    private VanillaPhantomDamageResolver() {}

    @Override
    public PhantomDamageResult apply(PhantomDamageContext context) {
        float damage =
                calculateDamage(
                        context.attacker().getAttributeValue(Attributes.ATTACK_DAMAGE),
                        context.damageMultiplier());
        if (damage <= 0.0F) {
            return PhantomDamageResult.rejected("non_positive_damage");
        }
        if (!context.target()
                .hurt(
                        context.attacker().damageSources().playerAttack(context.attacker()),
                        damage)) {
            return PhantomDamageResult.rejected("target_hurt_rejected");
        }

        context.attacker().resetAttackStrengthTicker();
        context.attacker().swing(InteractionHand.MAIN_HAND, true);
        return PhantomDamageResult.applied(damage);
    }

    static float calculateDamage(double baseAttackDamage, float multiplier) {
        return (float) (baseAttackDamage * multiplier);
    }
}
