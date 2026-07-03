package com.time_engine.sandevistan.registry;

import com.time_engine.TimeEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TimeEngine.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TIME_ENGINE =
            TABS.register("time_engine", ModCreativeTabs::createTimeEngineTab);

    private ModCreativeTabs() {}

    private static CreativeModeTab createTimeEngineTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.time_engine"))
                .icon(ModCreativeTabs::icon)
                .displayItems(ModCreativeTabs::displayItems)
                .build();
    }

    private static ItemStack icon() {
        return new ItemStack(ModItems.SANDEVISTAN_MK3.get());
    }

    private static void displayItems(
            CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(ModItems.SANDEVISTAN_MK1.get());
        output.accept(ModItems.SANDEVISTAN_MK2.get());
        output.accept(ModItems.SANDEVISTAN_MK3.get());
    }
}
