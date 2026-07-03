package com.time_engine.sandevistan.item;

import com.time_engine.api.TemporalEngineApi;
import com.time_engine.api.TemporalSessionOptions;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class SandevistanItem extends Item {
    private final SandevistanTier tier;

    public SandevistanItem(SandevistanTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (TemporalEngineApi.sessionState(serverPlayer).isPresent()) {
            return TemporalEngineApi.stop(serverPlayer).success()
                    ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.fail(stack);
        }

        return TemporalEngineApi.activate(serverPlayer, tier.options()).success()
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TemporalSessionOptions options = tier.options();
        tooltip.add(
                Component.translatable("tooltip.time_engine.sandevistan.tier", tierName())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.time_engine.sandevistan.duration",
                                formatSeconds(options.durationTicks()))
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.time_engine.sandevistan.cooldown",
                                formatSeconds(options.cooldownTicks()))
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.time_engine.sandevistan.time_scale", options.timeScale())
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.time_engine.sandevistan.radius",
                                formatDecimal(options.radius()))
                        .withStyle(ChatFormatting.GRAY));
    }

    private Component tierName() {
        return Component.translatable(tier.displayKey());
    }

    private static String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
