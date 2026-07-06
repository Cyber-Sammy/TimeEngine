package com.time_engine.engine.client;

import com.time_engine.TimeEngine;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = TimeEngine.MOD_ID, value = Dist.CLIENT)
public final class EngineClientModEvents {
    private EngineClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(TimeEngine.MOD_ID, "causal_link_debug"),
                CausalLinkDebugHud::render);
    }
}
