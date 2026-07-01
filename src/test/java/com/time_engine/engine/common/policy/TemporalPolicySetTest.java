package com.time_engine.engine.common.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicy.TargetKind;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class TemporalPolicySetTest {
    private static final ResourceLocation ZOMBIE = location("minecraft", "zombie");

    @Test
    void higherPriorityMatchingPolicyWins() {
        TemporalPolicy lower = policy("lower", 10, Set.of(ZOMBIE), Set.of(), Decision.ALLOW);
        TemporalPolicy higher = policy("higher", 20, Set.of(ZOMBIE), Set.of(), Decision.IGNORE);
        TemporalPolicySet policySet = new TemporalPolicySet(List.of(lower, higher));

        TemporalPolicySet.Resolution resolution =
                policySet.resolve(
                        TargetKind.ENTITY, ZOMBIE, Set.of(), Operation.SNAPSHOT, Decision.ALLOW);

        assertEquals(Decision.IGNORE, resolution.decision());
        assertEquals(location("test", "higher"), resolution.policyId().orElseThrow());
    }

    @Test
    void tagSelectorMatches() {
        ResourceLocation undead = location("test", "undead");
        TemporalPolicy policy = policy("tagged", 0, Set.of(), Set.of(undead), Decision.IGNORE);
        TemporalPolicySet policySet = new TemporalPolicySet(List.of(policy));

        TemporalPolicySet.Resolution resolution =
                policySet.resolve(
                        TargetKind.ENTITY,
                        ZOMBIE,
                        Set.of(undead),
                        Operation.SNAPSHOT,
                        Decision.ALLOW);

        assertEquals(Decision.IGNORE, resolution.decision());
    }

    @Test
    void samePriorityUsesStablePolicyIdOrder() {
        TemporalPolicy laterId = policy("z_rule", 10, Set.of(ZOMBIE), Set.of(), Decision.ALLOW);
        TemporalPolicy earlierId = policy("a_rule", 10, Set.of(ZOMBIE), Set.of(), Decision.IGNORE);
        TemporalPolicySet policySet = new TemporalPolicySet(List.of(laterId, earlierId));

        TemporalPolicySet.Resolution resolution =
                policySet.resolve(
                        TargetKind.ENTITY, ZOMBIE, Set.of(), Operation.SNAPSHOT, Decision.ALLOW);

        assertEquals(location("test", "a_rule"), resolution.policyId().orElseThrow());
        assertEquals(Decision.IGNORE, resolution.decision());
    }

    @Test
    void returnsFallbackWhenNoRuleMatches() {
        TemporalPolicySet policySet = new TemporalPolicySet(List.of());

        TemporalPolicySet.Resolution resolution =
                policySet.resolve(
                        TargetKind.ENTITY, ZOMBIE, Set.of(), Operation.SNAPSHOT, Decision.ALLOW);

        assertEquals(Decision.ALLOW, resolution.decision());
        assertTrue(resolution.policyId().isEmpty());
    }

    @Test
    void skipsMatchingRuleThatDoesNotDefineRequestedOperation() {
        TemporalPolicy highPriorityCombatOnly =
                new TemporalPolicy(
                        location("test", "combat_only"),
                        TargetKind.ENTITY,
                        100,
                        Set.of(ZOMBIE),
                        Set.of(),
                        Map.of(Operation.PHANTOM_COMBAT, Decision.IGNORE));
        TemporalPolicy snapshotRule =
                policy("snapshot", 10, Set.of(ZOMBIE), Set.of(), Decision.ALLOW);
        TemporalPolicySet policySet =
                new TemporalPolicySet(List.of(highPriorityCombatOnly, snapshotRule));

        TemporalPolicySet.Resolution resolution =
                policySet.resolve(
                        TargetKind.ENTITY, ZOMBIE, Set.of(), Operation.SNAPSHOT, Decision.IGNORE);

        assertEquals(Decision.ALLOW, resolution.decision());
        assertEquals(location("test", "snapshot"), resolution.policyId().orElseThrow());
    }

    private static TemporalPolicy policy(
            String name,
            int priority,
            Set<ResourceLocation> ids,
            Set<ResourceLocation> tags,
            Decision decision) {
        return new TemporalPolicy(
                location("test", name),
                TargetKind.ENTITY,
                priority,
                ids,
                tags,
                Map.of(Operation.SNAPSHOT, decision));
    }

    private static ResourceLocation location(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
