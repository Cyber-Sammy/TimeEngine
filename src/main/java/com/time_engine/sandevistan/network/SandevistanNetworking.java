package com.time_engine.sandevistan.network;

import com.time_engine.sandevistan.activation.SandevistanActivationService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SandevistanNetworking {
    public static final String PROTOCOL_VERSION = "1";

    private SandevistanNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToServer(
                        SandevistanActivationRequestPayload.TYPE,
                        SandevistanActivationRequestPayload.STREAM_CODEC,
                        SandevistanNetworking::handleActivationRequest);
    }

    private static void handleActivationRequest(
            SandevistanActivationRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        SandevistanActivationService.toggleBestAvailable(serverPlayer);
    }
}
