package com.time_engine.engine.common.causal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record CausalPhantomEquipmentState(
        ResourceLocation mainHand, ResourceLocation offHand, List<ResourceLocation> armorItems) {
    public CausalPhantomEquipmentState {
        Objects.requireNonNull(armorItems, "armorItems");
        armorItems = Collections.unmodifiableList(new ArrayList<>(armorItems));
    }

    public static final CausalPhantomEquipmentState EMPTY =
            new CausalPhantomEquipmentState(null, null, List.of());

    public static CausalPhantomEquipmentState capture(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return EMPTY;
        }
        return fromLivingEntity(livingEntity);
    }

    public static CausalPhantomEquipmentState fromStacks(
            ItemStack mainHand, ItemStack offHand, List<ItemStack> armorItems) {
        Objects.requireNonNull(armorItems, "armorItems");
        return new CausalPhantomEquipmentState(
                itemId(mainHand),
                itemId(offHand),
                armorItems.stream().map(CausalPhantomEquipmentState::itemId).toList());
    }

    private static CausalPhantomEquipmentState fromLivingEntity(LivingEntity livingEntity) {
        return fromStacks(
                livingEntity.getMainHandItem(),
                livingEntity.getOffhandItem(),
                List.of(
                        livingEntity.getItemBySlot(EquipmentSlot.HEAD),
                        livingEntity.getItemBySlot(EquipmentSlot.CHEST),
                        livingEntity.getItemBySlot(EquipmentSlot.LEGS),
                        livingEntity.getItemBySlot(EquipmentSlot.FEET)));
    }

    private static ResourceLocation itemId(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
