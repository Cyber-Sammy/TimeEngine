package com.time_engine.common.policy;

import com.time_engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.common.policy.TemporalPolicy.TargetKind;
import com.time_engine.common.policy.TemporalPolicySet.Resolution;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public final class TemporalPolicyResolver {
    private static final TemporalPolicyResolver INSTANCE = new TemporalPolicyResolver();

    private final AtomicLong generationCounter = new AtomicLong();
    private volatile State state =
            new State(new TemporalPolicySet(Set.of()), new ReloadStats(0, 0, 0));

    private TemporalPolicyResolver() {}

    public static TemporalPolicyResolver getInstance() {
        return INSTANCE;
    }

    public ResolvedPolicy resolveEntity(Entity entity, Operation operation, Decision fallback) {
        State currentState = state;
        if (currentState.policySet().size() == 0) {
            return ResolvedPolicy.fallback(fallback);
        }
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Set<ResourceLocation> tags =
                currentState.policySet().targetTags(TargetKind.ENTITY).stream()
                        .filter(
                                tagId ->
                                        entity.getType()
                                                .is(TagKey.create(Registries.ENTITY_TYPE, tagId)))
                        .collect(Collectors.toUnmodifiableSet());
        return toPublicResolution(
                currentState
                        .policySet()
                        .resolve(TargetKind.ENTITY, entityTypeId, tags, operation, fallback));
    }

    public ResolvedPolicy resolveBlock(
            BlockState blockState, Operation operation, Decision fallback) {
        State currentState = state;
        if (currentState.policySet().size() == 0) {
            return ResolvedPolicy.fallback(fallback);
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        Set<ResourceLocation> tags =
                currentState.policySet().targetTags(TargetKind.BLOCK).stream()
                        .filter(tagId -> blockState.is(TagKey.create(Registries.BLOCK, tagId)))
                        .collect(Collectors.toUnmodifiableSet());
        return toPublicResolution(
                currentState
                        .policySet()
                        .resolve(TargetKind.BLOCK, blockId, tags, operation, fallback));
    }

    public ReloadStats stats() {
        return state.stats();
    }

    void replacePolicies(Collection<TemporalPolicy> policies, int rejectedPolicies) {
        TemporalPolicySet policySet = new TemporalPolicySet(policies);
        long generation = generationCounter.incrementAndGet();
        state =
                new State(
                        policySet, new ReloadStats(policySet.size(), rejectedPolicies, generation));
    }

    private static ResolvedPolicy toPublicResolution(Resolution resolution) {
        return new ResolvedPolicy(resolution.decision(), resolution.policyId());
    }

    private record State(TemporalPolicySet policySet, ReloadStats stats) {}

    public record ResolvedPolicy(Decision decision, Optional<ResourceLocation> policyId) {
        static ResolvedPolicy fallback(Decision decision) {
            return new ResolvedPolicy(decision, Optional.empty());
        }
    }

    public record ReloadStats(int loadedPolicies, int rejectedPolicies, long generation) {}
}
