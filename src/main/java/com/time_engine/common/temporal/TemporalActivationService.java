package com.time_engine.common.temporal;

import com.time_engine.common.network.ModNetworking;
import com.time_engine.config.TemporalConfigService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalActivationService {
    private TemporalActivationService() {}

    public static boolean toggle(ServerPlayer player) {
        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        if (manager.isActive(player)) {
            manager.stopSession(player);
            ModNetworking.sendState(player);
            player.displayClientMessage(
                    Component.translatable("message.time_engine.temporal.stopped"), true);
            return true;
        }

        if (manager.startSession(player)) {
            ModNetworking.sendState(player);
            double durationSeconds =
                    TemporalConfigService.sessionSettings(player).durationTicks() / 20.0D;
            player.displayClientMessage(
                    Component.translatable("message.time_engine.temporal.started", durationSeconds),
                    true);
            return true;
        }

        int cooldownTicks = manager.getCooldownTicksRemaining(player);
        ModNetworking.sendState(player);
        player.displayClientMessage(
                Component.translatable("message.time_engine.temporal.cooldown", cooldownTicks),
                true);
        return false;
    }
}
