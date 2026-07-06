package com.time_engine;

import com.time_engine.engine.common.network.ModNetworking;
import com.time_engine.engine.config.TimeEngineClientConfig;
import com.time_engine.engine.config.TimeEngineConfig;
import com.time_engine.sandevistan.network.SandevistanNetworking;
import com.time_engine.sandevistan.registry.ModCreativeTabs;
import com.time_engine.sandevistan.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(TimeEngine.MOD_ID)
public final class TimeEngine {
    public static final String MOD_ID = "time_engine";

    public TimeEngine(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, TimeEngineConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, TimeEngineClientConfig.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, TimeEngineConfig.SERVER_SPEC);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(SandevistanNetworking::register);
    }
}
