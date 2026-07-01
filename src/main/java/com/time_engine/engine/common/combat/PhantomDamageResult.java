package com.time_engine.engine.common.combat;

public record PhantomDamageResult(boolean applied, float damage, String reason) {
    public static PhantomDamageResult applied(float damage) {
        return new PhantomDamageResult(true, damage, "applied");
    }

    public static PhantomDamageResult rejected(String reason) {
        return new PhantomDamageResult(false, 0.0F, reason);
    }
}
