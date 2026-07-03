package com.time_engine.engine.common.causal;

import java.util.Optional;

public record CausalTargetSelection(
        Optional<CausalTargetCandidate> selectedCandidate, double selectedScore) {
    public static CausalTargetSelection empty() {
        return new CausalTargetSelection(Optional.empty(), 0.0D);
    }

    public static CausalTargetSelection selected(
            CausalTargetCandidate candidate, double selectedScore) {
        return new CausalTargetSelection(Optional.of(candidate), selectedScore);
    }
}
