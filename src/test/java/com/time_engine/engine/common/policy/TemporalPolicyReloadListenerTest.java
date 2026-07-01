package com.time_engine.engine.common.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.time_engine.engine.common.policy.TemporalPolicyResolver.ReloadStats;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TemporalPolicyReloadListenerTest {
    private final AtomicInteger runtimeResetCount = new AtomicInteger();
    private final TemporalPolicyReloadListener listener =
            new TemporalPolicyReloadListener(runtimeResetCount::incrementAndGet);

    @AfterEach
    void clearPolicies() {
        listener.apply(Map.of(), null, null);
    }

    @Test
    void atomicallyPublishesValidPoliciesAndRejectsInvalidEntries() {
        Map<ResourceLocation, JsonElement> resources =
                Map.of(
                        location("valid"),
                        json(
                                """
                                {
                                  "target": "entity",
                                  "ids": ["minecraft:zombie"],
                                  "operations": {"snapshot": "ignore"}
                                }
                                """),
                        location("invalid"),
                        json(
                                """
                                {
                                  "target": "block",
                                  "operations": {"interaction": "lock_interaction"}
                                }
                                """));

        listener.apply(resources, null, null);

        ReloadStats stats = TemporalPolicyResolver.getInstance().stats();
        assertEquals(1, stats.loadedPolicies());
        assertEquals(1, stats.rejectedPolicies());
        assertTrue(stats.generation() > 0);
        assertEquals(1, runtimeResetCount.get());
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static JsonElement json(String value) {
        return JsonParser.parseString(value);
    }
}
