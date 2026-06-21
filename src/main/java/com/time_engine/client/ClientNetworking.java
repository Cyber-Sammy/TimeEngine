package com.time_engine.client;

import com.time_engine.TimeEngine;
import com.time_engine.common.network.AfterimagePayload;
import com.time_engine.common.network.GhostFramePayload;
import com.time_engine.common.network.ModNetworking;
import com.time_engine.common.network.TemporalConfigPayload;
import com.time_engine.common.network.TemporalStatePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = TimeEngine.MOD_ID, value = Dist.CLIENT)
public final class ClientNetworking {
    private ClientNetworking() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(ModNetworking.PROTOCOL_VERSION)
                .playToClient(
                        TemporalStatePayload.TYPE,
                        TemporalStatePayload.STREAM_CODEC,
                        ClientNetworking::handleTemporalState)
                .playToClient(
                        GhostFramePayload.TYPE,
                        GhostFramePayload.STREAM_CODEC,
                        ClientNetworking::handleGhostFrame)
                .playToClient(
                        AfterimagePayload.TYPE,
                        AfterimagePayload.STREAM_CODEC,
                        ClientNetworking::handleAfterimage)
                .playToClient(
                        TemporalConfigPayload.TYPE,
                        TemporalConfigPayload.STREAM_CODEC,
                        ClientNetworking::handleTemporalConfig);
    }

    private static void handleTemporalState(TemporalStatePayload payload, IPayloadContext context) {
        ClientTemporalState.apply(payload);
        if (!payload.active()) {
            ClientGhostState.clear();
        }
    }

    private static void handleGhostFrame(GhostFramePayload payload, IPayloadContext context) {
        ClientGhostState.apply(payload);
    }

    private static void handleAfterimage(AfterimagePayload payload, IPayloadContext context) {
        ClientAfterimageState.apply(payload);
    }

    private static void handleTemporalConfig(
            TemporalConfigPayload payload, IPayloadContext context) {
        TemporalConfigScreen.receive(payload);
    }
}
