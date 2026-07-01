package com.time_engine.engine.common.policy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicy.TargetKind;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class TemporalPolicyParser {
    private TemporalPolicyParser() {}

    public static TemporalPolicy parse(ResourceLocation id, JsonElement json) {
        try {
            JsonObject root = requireObject(json, "policy");
            TargetKind targetKind = TargetKind.parse(requireString(root, "target"));
            int priority = root.has("priority") ? root.get("priority").getAsInt() : 0;
            Set<ResourceLocation> targetIds = parseLocations(root, "ids");
            Set<ResourceLocation> targetTags = parseLocations(root, "tags");
            Map<Operation, Decision> decisions = parseDecisions(root, targetKind);
            return new TemporalPolicy(id, targetKind, priority, targetIds, targetTags, decisions);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new JsonParseException(
                    "Invalid temporal policy " + id + ": " + exception.getMessage(), exception);
        }
    }

    private static Map<Operation, Decision> parseDecisions(JsonObject root, TargetKind targetKind) {
        JsonObject operations = requireObject(root.get("operations"), "operations");
        Map<Operation, Decision> decisions = new EnumMap<>(Operation.class);
        for (Map.Entry<String, JsonElement> entry : operations.entrySet()) {
            Operation operation = Operation.parse(entry.getKey());
            Decision decision = Decision.parse(entry.getValue().getAsString());
            validateDecision(targetKind, operation, decision);
            decisions.put(operation, decision);
        }
        return decisions;
    }

    private static void validateDecision(
            TargetKind targetKind, Operation operation, Decision decision) {
        if (!operation.supports(targetKind)) {
            throw new IllegalArgumentException(operation + " does not support " + targetKind);
        }
        if (operation == Operation.INTERACTION) {
            validateInteractionDecision(decision);
            return;
        }
        if (decision == Decision.LOCK_INTERACTION) {
            throw new IllegalArgumentException(
                    "lock_interaction is only valid for the interaction operation");
        }
    }

    private static void validateInteractionDecision(Decision decision) {
        if (decision == Decision.IGNORE) {
            throw new IllegalArgumentException(
                    "interaction supports only allow or lock_interaction");
        }
    }

    private static Set<ResourceLocation> parseLocations(JsonObject root, String memberName) {
        Set<ResourceLocation> locations = new HashSet<>();
        if (!root.has(memberName)) {
            return locations;
        }
        for (JsonElement element : root.getAsJsonArray(memberName)) {
            locations.add(ResourceLocation.parse(element.getAsString()));
        }
        return locations;
    }

    private static String requireString(JsonObject object, String memberName) {
        if (!object.has(memberName)) {
            throw new IllegalArgumentException("Missing " + memberName);
        }
        return object.get(memberName).getAsString();
    }

    private static JsonObject requireObject(JsonElement element, String name) {
        if (element == null) {
            throw new IllegalArgumentException("Missing " + name);
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException(name + " must be an object");
        }
        return element.getAsJsonObject();
    }
}
