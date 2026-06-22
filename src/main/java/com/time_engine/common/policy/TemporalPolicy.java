package com.time_engine.common.policy;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record TemporalPolicy(
        ResourceLocation id,
        TargetKind targetKind,
        int priority,
        Set<ResourceLocation> targetIds,
        Set<ResourceLocation> targetTags,
        Map<Operation, Decision> decisions) {
    public TemporalPolicy {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(targetKind, "targetKind");
        targetIds = Set.copyOf(targetIds);
        targetTags = Set.copyOf(targetTags);
        decisions = Map.copyOf(decisions);
        if (!hasSelector(targetIds, targetTags)) {
            throw new IllegalArgumentException("Policy must select at least one id or tag");
        }
        if (decisions.isEmpty()) {
            throw new IllegalArgumentException("Policy must define at least one operation");
        }
    }

    private static boolean hasSelector(
            Set<ResourceLocation> targetIds, Set<ResourceLocation> targetTags) {
        if (!targetIds.isEmpty()) {
            return true;
        }
        return !targetTags.isEmpty();
    }

    public Optional<Decision> decision(Operation operation) {
        return Optional.ofNullable(decisions.get(operation));
    }

    boolean matches(ResourceLocation targetId, Set<ResourceLocation> tags) {
        if (targetIds.contains(targetId)) {
            return true;
        }
        for (ResourceLocation tag : tags) {
            if (targetTags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public enum TargetKind {
        ENTITY("entity"),
        BLOCK("block");

        private final String serializedName;

        TargetKind(String serializedName) {
            this.serializedName = serializedName;
        }

        static TargetKind parse(String value) {
            for (TargetKind kind : values()) {
                if (kind.serializedName.equals(value)) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("Unknown policy target: " + value);
        }
    }

    public enum Operation {
        SNAPSHOT("snapshot", TargetKind.ENTITY),
        PHANTOM_COMBAT("phantom_combat", TargetKind.ENTITY),
        TEMPORAL_INTERCEPT("temporal_intercept", null),
        INTERACTION("interaction", TargetKind.BLOCK);

        private final String serializedName;
        private final TargetKind requiredTargetKind;

        Operation(String serializedName, TargetKind requiredTargetKind) {
            this.serializedName = serializedName;
            this.requiredTargetKind = requiredTargetKind;
        }

        static Operation parse(String value) {
            for (Operation operation : values()) {
                if (operation.serializedName.equals(value)) {
                    return operation;
                }
            }
            throw new IllegalArgumentException("Unknown temporal operation: " + value);
        }

        boolean supports(TargetKind kind) {
            if (requiredTargetKind == null) {
                return true;
            }
            return requiredTargetKind == kind;
        }
    }

    public enum Decision {
        ALLOW("allow"),
        IGNORE("ignore"),
        LOCK_INTERACTION("lock_interaction");

        private final String serializedName;

        Decision(String serializedName) {
            this.serializedName = serializedName;
        }

        static Decision parse(String value) {
            for (Decision decision : values()) {
                if (decision.serializedName.equals(value)) {
                    return decision;
                }
            }
            throw new IllegalArgumentException("Unknown policy decision: " + value);
        }
    }
}
