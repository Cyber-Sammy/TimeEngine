package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class CausalPhantomEquipmentStateTest {
    @Test
    void fromStacksRecordsHeldAndArmorItemIds() {
        CausalPhantomEquipmentState state =
                CausalPhantomEquipmentState.fromStacks(
                        new ItemStack(Items.DIAMOND_SWORD),
                        new ItemStack(Items.SHIELD),
                        List.of(
                                new ItemStack(Items.DIAMOND_HELMET),
                                new ItemStack(Items.DIAMOND_CHESTPLATE),
                                new ItemStack(Items.DIAMOND_LEGGINGS),
                                new ItemStack(Items.DIAMOND_BOOTS)));

        assertEquals(itemId(Items.DIAMOND_SWORD), state.mainHand());
        assertEquals(itemId(Items.SHIELD), state.offHand());
        assertEquals(itemId(Items.DIAMOND_HELMET), state.armorItems().getFirst());
        assertEquals(itemId(Items.DIAMOND_BOOTS), state.armorItems().getLast());
    }

    @Test
    void fromStacksTreatsEmptyItemsAsAbsent() {
        CausalPhantomEquipmentState state =
                CausalPhantomEquipmentState.fromStacks(
                        ItemStack.EMPTY, ItemStack.EMPTY, List.of(ItemStack.EMPTY));

        assertNull(state.mainHand());
        assertNull(state.offHand());
        assertNull(state.armorItems().getFirst());
    }

    private static ResourceLocation itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
