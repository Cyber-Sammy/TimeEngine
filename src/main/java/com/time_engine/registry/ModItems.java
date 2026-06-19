package com.time_engine.registry;

import com.time_engine.TimeEngine;
import com.time_engine.common.item.DebugTemporalItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeEngine.MOD_ID);

    public static final DeferredItem<Item> DEBUG_ITEM = ITEMS.register(
            "debug_item",
            () -> new DebugTemporalItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
    );

    private ModItems() {
    }
}
