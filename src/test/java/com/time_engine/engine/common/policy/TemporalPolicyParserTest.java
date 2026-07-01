package com.time_engine.engine.common.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicy.TargetKind;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class TemporalPolicyParserTest {
    private static final ResourceLocation POLICY_ID =
            ResourceLocation.fromNamespaceAndPath("test", "zombie");

    @Test
    void parsesEntityPolicy() {
        TemporalPolicy policy =
                parse(
                        """
                        {
                          "target": "entity",
                          "priority": 50,
                          "ids": ["minecraft:zombie"],
                          "tags": ["minecraft:skeletons"],
                          "operations": {
                            "snapshot": "allow",
                            "phantom_combat": "ignore"
                          }
                        }
                        """);

        assertEquals(TargetKind.ENTITY, policy.targetKind());
        assertEquals(50, policy.priority());
        assertEquals(Decision.ALLOW, policy.decision(Operation.SNAPSHOT).orElseThrow());
        assertEquals(Decision.IGNORE, policy.decision(Operation.PHANTOM_COMBAT).orElseThrow());
    }

    @Test
    void rejectsPolicyWithoutSelector() {
        assertThrows(
                JsonParseException.class,
                () ->
                        parse(
                                """
                                {
                                  "target": "entity",
                                  "operations": {"snapshot": "allow"}
                                }
                                """));
    }

    @Test
    void rejectsOperationForWrongTargetKind() {
        assertThrows(
                JsonParseException.class,
                () ->
                        parse(
                                """
                                {
                                  "target": "block",
                                  "ids": ["minecraft:stone"],
                                  "operations": {"snapshot": "allow"}
                                }
                                """));
    }

    @Test
    void rejectsInteractionLockForNonInteractionOperation() {
        assertThrows(
                JsonParseException.class,
                () ->
                        parse(
                                """
                                {
                                  "target": "entity",
                                  "ids": ["minecraft:zombie"],
                                  "operations": {"snapshot": "lock_interaction"}
                                }
                                """));
    }

    @Test
    void acceptsInteractionLockForBlockInteraction() {
        TemporalPolicy policy =
                parse(
                        """
                        {
                          "target": "block",
                          "ids": ["minecraft:chest"],
                          "operations": {"interaction": "lock_interaction"}
                        }
                        """);

        assertEquals(
                Decision.LOCK_INTERACTION, policy.decision(Operation.INTERACTION).orElseThrow());
    }

    @Test
    void rejectsIgnoreForBlockInteraction() {
        assertThrows(
                JsonParseException.class,
                () ->
                        parse(
                                """
                                {
                                  "target": "block",
                                  "ids": ["minecraft:chest"],
                                  "operations": {"interaction": "ignore"}
                                }
                                """));
    }

    private static TemporalPolicy parse(String json) {
        return TemporalPolicyParser.parse(POLICY_ID, JsonParser.parseString(json));
    }
}
