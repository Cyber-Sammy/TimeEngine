package com.time_engine.client;

import com.time_engine.TimeEngine;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = TimeEngine.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasActiveWorld(minecraft)) {
            ClientTemporalState.reset();
            ClientGhostState.clear();
            return;
        }
        if (!ClientTemporalState.isActive()) {
            ClientGhostState.clear();
        }
        TimeEngineKeyMappings.handleInput();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        GhostDebugRenderer.render(event);
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        if (!ClientGhostTargeting.tryAttackNearestGhost()) {
            return;
        }
        event.setCanceled(true);
    }

    private static boolean hasActiveWorld(Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.level != null;
    }
}
