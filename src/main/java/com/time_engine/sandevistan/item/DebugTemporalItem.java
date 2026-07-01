package com.time_engine.sandevistan.item;

import com.time_engine.api.TemporalEngineApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DebugTemporalItem extends Item {
    public DebugTemporalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return TemporalEngineApi.toggleTemporalSession(serverPlayer)
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.fail(stack);
    }
}
