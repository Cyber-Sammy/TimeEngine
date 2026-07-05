package com.time_engine.engine.common.causal;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record CausalPhantomEquipmentState(
        ResourceLocation mainHand, ResourceLocation offHand, List<ResourceLocation> armorItems) {
    public CausalPhantomEquipmentState {
        armorItems = List.copyOf(armorItems);
    }

    public static final CausalPhantomEquipmentState EMPTY =
            new CausalPhantomEquipmentState(null, null, List.of());
}
