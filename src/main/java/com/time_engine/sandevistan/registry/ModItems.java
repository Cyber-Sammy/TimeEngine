package com.time_engine.sandevistan.registry;

import com.time_engine.TimeEngine;
import com.time_engine.sandevistan.item.DebugTemporalItem;
import com.time_engine.sandevistan.item.SandevistanItem;
import com.time_engine.sandevistan.item.SandevistanTier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TimeEngine.MOD_ID);

    public static final DeferredItem<Item> SANDEVISTAN_MK1 =
            registerSandevistan(SandevistanTier.MK1);
    public static final DeferredItem<Item> SANDEVISTAN_MK2 =
            registerSandevistan(SandevistanTier.MK2);
    public static final DeferredItem<Item> SANDEVISTAN_MK3 =
            registerSandevistan(SandevistanTier.MK3);

    public static final DeferredItem<Item> DEBUG_ITEM =
            ITEMS.register(
                    "debug_item",
                    () ->
                            new DebugTemporalItem(
                                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private ModItems() {}

    private static DeferredItem<Item> registerSandevistan(SandevistanTier tier) {
        return ITEMS.register(
                "sandevistan_" + tier.id(),
                () ->
                        new SandevistanItem(
                                tier, new Item.Properties().stacksTo(1).rarity(tier.rarity())));
    }
}
