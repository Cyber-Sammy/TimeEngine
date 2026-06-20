package com.time_engine.common.item;

import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import net.minecraft.network.chat.Component;
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

        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        if (manager.isActive(serverPlayer)) {
            manager.stopSession(serverPlayer);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.time_engine.temporal.stopped"), true);
            return InteractionResultHolder.success(stack);
        }

        if (manager.startSession(serverPlayer)) {
            double durationSeconds = TimeEngineConfig.durationTicks() / 20.0D;
            serverPlayer.displayClientMessage(
                    Component.translatable("message.time_engine.temporal.started", durationSeconds),
                    true);
            return InteractionResultHolder.success(stack);
        }

        int cooldownTicks = manager.getCooldownTicksRemaining(serverPlayer);
        serverPlayer.displayClientMessage(
                Component.translatable("message.time_engine.temporal.cooldown", cooldownTicks),
                true);
        return InteractionResultHolder.fail(stack);
    }
}
