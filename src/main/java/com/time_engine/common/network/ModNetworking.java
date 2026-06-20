package com.time_engine.common.network;

import com.time_engine.common.temporal.TemporalActivationService;
import com.time_engine.common.temporal.TemporalSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetworking {
    public static final String PROTOCOL_VERSION = "2";

    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                TemporalActivationRequestPayload.TYPE,
                TemporalActivationRequestPayload.STREAM_CODEC,
                ModNetworking::handleActivationRequest);

        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            registrar
                    .playToClient(
                            TemporalStatePayload.TYPE,
                            TemporalStatePayload.STREAM_CODEC,
                            (payload, context) -> {})
                    .playToClient(
                            GhostFramePayload.TYPE,
                            GhostFramePayload.STREAM_CODEC,
                            (payload, context) -> {});
        }
    }

    public static void sendState(ServerPlayer player) {
        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        int serverTick = player.getServer().getTickCount();
        int cooldownEndTick = manager.getCooldownEndTick(player);
        TemporalStatePayload payload =
                manager.getSession(player)
                        .map(
                                session ->
                                        TemporalStatePayload.active(
                                                session, serverTick, cooldownEndTick))
                        .orElseGet(
                                () -> TemporalStatePayload.inactive(serverTick, cooldownEndTick));
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void handleActivationRequest(
            TemporalActivationRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            TemporalActivationService.toggle(serverPlayer);
        }
    }
}
