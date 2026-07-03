package com.time_engine.engine.common.causal;

public record CausalPhantomActionState(
        boolean swinging, boolean attacking, boolean blocking, boolean usingItem) {
    public static final CausalPhantomActionState NONE =
            new CausalPhantomActionState(false, false, false, false);
}
