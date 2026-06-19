package com.time_engine.common.temporal;

import com.time_engine.TimeEngine;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TimeEngine.MOD_ID)
public final class TemporalServerEvents {
    private TemporalServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TemporalSessionManager.getInstance().tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TemporalSessionManager.getInstance().clear();
    }
}
