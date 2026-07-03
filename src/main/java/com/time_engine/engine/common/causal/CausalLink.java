package com.time_engine.engine.common.causal;

import java.util.UUID;

public record CausalLink(
        UUID ownerId,
        UUID targetId,
        CausalLinkState state,
        int selectedTick,
        int lastUpdatedTick,
        int hardLockUntilTick,
        CausalPhantomFrame latestFrame,
        double progress) {
    public CausalLink {
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
        progress = Math.max(0.0D, Math.min(1.0D, progress));
    }

    public static CausalLink softLocked(
            UUID ownerId, UUID targetId, int serverTick, CausalPhantomFrame latestFrame) {
        return new CausalLink(
                ownerId,
                targetId,
                CausalLinkState.SOFT_LOCK,
                serverTick,
                serverTick,
                serverTick,
                latestFrame,
                0.0D);
    }

    public CausalLink refreshSoftLock(
            UUID newTargetId, int serverTick, CausalPhantomFrame frame, double newProgress) {
        CausalLinkState refreshedState =
                hardLockedAt(serverTick) && targetId.equals(newTargetId)
                        ? CausalLinkState.HARD_LOCK
                        : CausalLinkState.SOFT_LOCK;
        return new CausalLink(
                ownerId,
                newTargetId,
                refreshedState,
                targetId.equals(newTargetId) ? selectedTick : serverTick,
                serverTick,
                hardLockUntilTick,
                frame,
                newProgress);
    }

    public CausalLink hardLock(int serverTick, int hardLockTicks) {
        return new CausalLink(
                ownerId,
                targetId,
                CausalLinkState.HARD_LOCK,
                selectedTick,
                serverTick,
                serverTick + Math.max(0, hardLockTicks),
                latestFrame,
                progress);
    }

    public CausalLink expire(int serverTick) {
        return terminal(CausalLinkState.EXPIRED, serverTick);
    }

    public CausalLink breakLink(int serverTick) {
        return terminal(CausalLinkState.BROKEN, serverTick);
    }

    public boolean active() {
        return state == CausalLinkState.SOFT_LOCK || state == CausalLinkState.HARD_LOCK;
    }

    public boolean hardLockedAt(int serverTick) {
        return state == CausalLinkState.HARD_LOCK && serverTick < hardLockUntilTick;
    }

    private CausalLink terminal(CausalLinkState terminalState, int serverTick) {
        return new CausalLink(
                ownerId,
                targetId,
                terminalState,
                selectedTick,
                serverTick,
                hardLockUntilTick,
                latestFrame,
                progress);
    }
}
