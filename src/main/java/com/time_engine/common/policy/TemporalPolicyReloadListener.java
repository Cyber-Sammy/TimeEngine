package com.time_engine.common.policy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.time_engine.util.ModLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class TemporalPolicyReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "time_engine/temporal_policies";

    public TemporalPolicyReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        List<TemporalPolicy> policies = new ArrayList<>();
        int rejectedPolicies = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                policies.add(TemporalPolicyParser.parse(entry.getKey(), entry.getValue()));
            } catch (JsonParseException exception) {
                rejectedPolicies++;
                ModLog.warn("Skipping {}: {}", entry.getKey(), exception.getMessage());
            }
        }

        TemporalPolicyResolver.getInstance().replacePolicies(policies, rejectedPolicies);
        ModLog.diagnostic(
                "Reloaded temporal policies: loaded={}, rejected={}",
                policies.size(),
                rejectedPolicies);
    }
}
