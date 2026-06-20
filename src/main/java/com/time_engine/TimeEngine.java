package com.time_engine;

import com.time_engine.config.TimeEngineConfig;
import com.time_engine.registry.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(TimeEngine.MOD_ID)
public final class TimeEngine {
    public static final String MOD_ID = "time_engine";

    public TimeEngine(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, TimeEngineConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, TimeEngineConfig.SERVER_SPEC);
        modEventBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DEBUG_ITEM);
        }
    }
}
