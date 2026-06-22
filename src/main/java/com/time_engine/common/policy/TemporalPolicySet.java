package com.time_engine.common.policy;

import com.time_engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.common.policy.TemporalPolicy.TargetKind;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

final class TemporalPolicySet {
    private static final Comparator<TemporalPolicy> POLICY_ORDER =
            Comparator.comparingInt(TemporalPolicy::priority)
                    .reversed()
                    .thenComparing(policy -> policy.id().toString());

    private final List<TemporalPolicy> policies;
    private final Map<TargetKind, Set<ResourceLocation>> targetTagsByKind;

    TemporalPolicySet(Collection<TemporalPolicy> policies) {
        this.policies = policies.stream().sorted(POLICY_ORDER).toList();
        this.targetTagsByKind = collectTargetTags(this.policies);
    }

    Resolution resolve(
            TargetKind targetKind,
            ResourceLocation targetId,
            Set<ResourceLocation> targetTags,
            Operation operation,
            Decision fallback) {
        for (TemporalPolicy policy : policies) {
            if (policy.targetKind() != targetKind) {
                continue;
            }
            Optional<Decision> decision = policy.decision(operation);
            if (decision.isEmpty()) {
                continue;
            }
            if (!policy.matches(targetId, targetTags)) {
                continue;
            }
            return new Resolution(decision.orElseThrow(), Optional.of(policy.id()));
        }
        return new Resolution(fallback, Optional.empty());
    }

    int size() {
        return policies.size();
    }

    Set<ResourceLocation> targetTags(TargetKind targetKind) {
        return targetTagsByKind.getOrDefault(targetKind, Set.of());
    }

    private static Map<TargetKind, Set<ResourceLocation>> collectTargetTags(
            List<TemporalPolicy> policies) {
        Map<TargetKind, Set<ResourceLocation>> mutableTags = new EnumMap<>(TargetKind.class);
        for (TemporalPolicy policy : policies) {
            mutableTags
                    .computeIfAbsent(policy.targetKind(), ignored -> new HashSet<>())
                    .addAll(policy.targetTags());
        }

        Map<TargetKind, Set<ResourceLocation>> immutableTags = new EnumMap<>(TargetKind.class);
        mutableTags.forEach((kind, tags) -> immutableTags.put(kind, Set.copyOf(tags)));
        return Map.copyOf(immutableTags);
    }

    record Resolution(Decision decision, Optional<ResourceLocation> policyId) {}
}
