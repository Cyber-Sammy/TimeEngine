package com.time_engine.engine.common.network;

import com.time_engine.engine.common.combat.TemporalCombatService;
import com.time_engine.engine.common.temporal.TemporalActivationService;
import com.time_engine.engine.common.temporal.TemporalSessionManager;
import com.time_engine.engine.config.TemporalConfigService;
import com.time_engine.engine.config.TemporalConfigSnapshot;
import com.time_engine.engine.config.TimeEngineConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetworking {
    public static final String PROTOCOL_VERSION = "7";

    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                TemporalActivationRequestPayload.TYPE,
                TemporalActivationRequestPayload.STREAM_CODEC,
                ModNetworking::handleActivationRequest);
        registrar.playToServer(
                PhantomHitRequestPayload.TYPE,
                PhantomHitRequestPayload.STREAM_CODEC,
                ModNetworking::handlePhantomHitRequest);
        registrar.playToServer(
                TemporalConfigUpdateRequestPayload.TYPE,
                TemporalConfigUpdateRequestPayload.STREAM_CODEC,
                ModNetworking::handleConfigUpdateRequest);

        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            registrar
                    .playToClient(
                            TemporalStatePayload.TYPE,
                            TemporalStatePayload.STREAM_CODEC,
                            (payload, context) -> {})
                    .playToClient(
                            GhostFramePayload.TYPE,
                            GhostFramePayload.STREAM_CODEC,
                            (payload, context) -> {})
                    .playToClient(
                            AfterimagePayload.TYPE,
                            AfterimagePayload.STREAM_CODEC,
                            (payload, context) -> {})
                    .playToClient(
                            TemporalConfigPayload.TYPE,
                            TemporalConfigPayload.STREAM_CODEC,
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
                                                session,
                                                serverTick,
                                                cooldownEndTick,
                                                TimeEngineConfig.phantomAttackReach()))
                        .orElseGet(
                                () ->
                                        TemporalStatePayload.inactive(
                                                serverTick,
                                                cooldownEndTick,
                                                TimeEngineConfig.phantomAttackReach()));
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendConfigScreen(ServerPlayer player, boolean success, String message) {
        PacketDistributor.sendToPlayer(
                player,
                new TemporalConfigPayload(
                        TemporalConfigService.currentFor(player),
                        TemporalConfigSnapshot.defaults(),
                        success,
                        message));
    }

    private static void handleActivationRequest(
            TemporalActivationRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            TemporalActivationService.toggle(serverPlayer);
        }
    }

    private static void handlePhantomHitRequest(
            PhantomHitRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            TemporalCombatService.getInstance().handle(serverPlayer, payload);
        }
    }

    private static void handleConfigUpdateRequest(
            TemporalConfigUpdateRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        TemporalConfigService.ApplyResult result =
                TemporalConfigService.apply(serverPlayer, payload.requested());
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new TemporalConfigPayload(
                        result.snapshot(),
                        TemporalConfigSnapshot.defaults(),
                        result.success(),
                        result.message()));
        if (result.success()) {
            serverPlayer.server.getPlayerList().getPlayers().forEach(ModNetworking::sendState);
        }
    }
}
