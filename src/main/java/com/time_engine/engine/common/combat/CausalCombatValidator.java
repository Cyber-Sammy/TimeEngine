package com.time_engine.engine.common.combat;

import com.time_engine.engine.common.causal.CausalLink;
import com.time_engine.engine.common.causal.CausalLinkManager;
import com.time_engine.engine.common.causal.CausalLinkState;
import com.time_engine.engine.common.causal.CausalTargetSelectionRules;
import java.util.Optional;
import java.util.UUID;

public final class CausalCombatValidator {
    private CausalCombatValidator() {}

    public static CausalCombatValidationResult validate(
            CausalLinkManager linkManager, UUID ownerId, UUID targetId) {
        Optional<CausalLink> link = linkManager.getLink(ownerId);
        if (link.isEmpty()) {
            return CausalCombatValidationResult.rejected(CausalCombatRejectionReason.NO_LINK);
        }

        CausalLink causalLink = link.orElseThrow();
        if (!causalLink.active()) {
            return rejectInactive(causalLink.state());
        }
        if (!causalLink.targetId().equals(targetId)) {
            return CausalCombatValidationResult.rejected(
                    CausalCombatRejectionReason.TARGET_MISMATCH);
        }

        return CausalCombatValidationResult.accepted(causalLink);
    }

    public static Optional<CausalLink> hardLockAcceptedHit(
            CausalLinkManager linkManager, UUID ownerId, int serverTick) {
        return linkManager.hardLock(
                ownerId, serverTick, CausalTargetSelectionRules.DEFAULT.hardLockTicks());
    }

    private static CausalCombatValidationResult rejectInactive(CausalLinkState state) {
        if (state == CausalLinkState.BROKEN) {
            return CausalCombatValidationResult.rejected(CausalCombatRejectionReason.BROKEN);
        }
        if (state == CausalLinkState.EXPIRED) {
            return CausalCombatValidationResult.rejected(CausalCombatRejectionReason.EXPIRED);
        }
        return CausalCombatValidationResult.rejected(CausalCombatRejectionReason.INACTIVE);
    }

    public enum CausalCombatRejectionReason {
        NONE,
        NO_LINK,
        TARGET_MISMATCH,
        BROKEN,
        EXPIRED,
        INACTIVE
    }

    public record CausalCombatValidationResult(
            CausalLink link, CausalCombatRejectionReason rejectionReason) {
        public static CausalCombatValidationResult accepted(CausalLink link) {
            return new CausalCombatValidationResult(link, CausalCombatRejectionReason.NONE);
        }

        public static CausalCombatValidationResult rejected(CausalCombatRejectionReason reason) {
            return new CausalCombatValidationResult(null, reason);
        }

        public boolean accepted() {
            return link != null;
        }
    }
}
