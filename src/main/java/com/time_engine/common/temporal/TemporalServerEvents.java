package com.time_engine.common.temporal;

import com.time_engine.TimeEngine;
import com.time_engine.common.combat.TemporalCombatService;
import com.time_engine.common.command.TemporalDebugCommands;
import com.time_engine.common.network.AfterimageBroadcaster;
import com.time_engine.common.network.GhostFrameBroadcaster;
import com.time_engine.common.network.ModNetworking;
import com.time_engine.common.snapshot.SnapshotManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TimeEngine.MOD_ID)
public final class TemporalServerEvents {
    private TemporalServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TemporalSessionManager.getInstance()
                .tick(event.getServer())
                .forEach(ModNetworking::sendState);
        SnapshotManager.getInstance().tick(event.getServer());
        GhostFrameBroadcaster.tick(event.getServer());
        AfterimageBroadcaster.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TemporalDebugCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.sendState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.sendState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TemporalCombatService.getInstance().clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SnapshotManager.getInstance().clear();
        TemporalSessionManager.getInstance().clear();
        TemporalCombatService.getInstance().clear();
    }
}
