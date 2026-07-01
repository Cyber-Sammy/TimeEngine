package com.time_engine.engine.common.policy;

import com.time_engine.TimeEngine;
import com.time_engine.engine.common.intercept.TemporalInterceptManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = TimeEngine.MOD_ID)
public final class TemporalPolicyEvents {
    private TemporalPolicyEvents() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new TemporalPolicyReloadListener());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!TemporalInterceptManager.getInstance().isInteractionLocked(level, event.getPos())) {
            return;
        }
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!TemporalInterceptManager.getInstance().isInteractionLocked(level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
    }
}
