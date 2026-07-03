package com.time_engine.engine.common.causal;

import java.util.UUID;

public record CausalLinkView(
        UUID ownerId,
        UUID targetId,
        CausalLinkState state,
        int selectedTick,
        int lastUpdatedTick,
        int ageTicks,
        int ticksSinceUpdate,
        int hardLockTicksRemaining,
        double progress) {
    public CausalLinkView {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId must not be null");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("targetId must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (!Double.isFinite(progress)) {
            throw new IllegalArgumentException("progress must be finite");
        }
        ageTicks = Math.max(0, ageTicks);
        ticksSinceUpdate = Math.max(0, ticksSinceUpdate);
        hardLockTicksRemaining = Math.max(0, hardLockTicksRemaining);
        progress = Math.max(0.0D, Math.min(1.0D, progress));
    }

    public static CausalLinkView from(CausalLink link, int serverTick) {
        return new CausalLinkView(
                link.ownerId(),
                link.targetId(),
                link.state(),
                link.selectedTick(),
                link.lastUpdatedTick(),
                serverTick - link.selectedTick(),
                serverTick - link.lastUpdatedTick(),
                link.hardLockUntilTick() - serverTick,
                link.progress());
    }

    public boolean active() {
        return state == CausalLinkState.SOFT_LOCK || state == CausalLinkState.HARD_LOCK;
    }
}
