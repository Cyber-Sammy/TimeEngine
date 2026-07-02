package com.time_engine.engine.common.temporal;

import com.time_engine.engine.common.network.ModNetworking;
import com.time_engine.engine.config.TemporalConfigService;
import com.time_engine.engine.config.TemporalSessionSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalActivationService {
    private TemporalActivationService() {}

    public static ActivationOutcome activate(ServerPlayer player) {
        return activate(player, TemporalConfigService.sessionSettings(player));
    }

    public static ActivationOutcome activate(
            ServerPlayer player, TemporalSessionSettings settings) {
        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        if (manager.startSession(player, settings)) {
            ModNetworking.sendState(player);
            double durationSeconds = settings.durationTicks() / 20.0D;
            player.displayClientMessage(
                    Component.translatable("message.time_engine.temporal.started", durationSeconds),
                    true);
            return ActivationOutcome.activated();
        }

        int cooldownTicks = manager.getCooldownTicksRemaining(player);
        ModNetworking.sendState(player);
        player.displayClientMessage(
                Component.translatable("message.time_engine.temporal.cooldown", cooldownTicks),
                true);
        return ActivationOutcome.cooldown(cooldownTicks);
    }

    public static ActivationOutcome stop(ServerPlayer player) {
        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        if (!manager.isActive(player)) {
            ModNetworking.sendState(player);
            return ActivationOutcome.stopped();
        }

        manager.stopSession(player);
        ModNetworking.sendState(player);
        player.displayClientMessage(
                Component.translatable("message.time_engine.temporal.stopped"), true);
        return ActivationOutcome.stopped();
    }

    public static ActivationOutcome toggle(ServerPlayer player) {
        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        if (manager.isActive(player)) {
            return stop(player);
        }

        return activate(player);
    }

    public record ActivationOutcome(boolean success, Status status, int cooldownTicksRemaining) {
        public static ActivationOutcome activated() {
            return new ActivationOutcome(true, Status.ACTIVATED, 0);
        }

        public static ActivationOutcome stopped() {
            return new ActivationOutcome(true, Status.STOPPED, 0);
        }

        public static ActivationOutcome cooldown(int cooldownTicksRemaining) {
            return new ActivationOutcome(false, Status.COOLDOWN, cooldownTicksRemaining);
        }
    }

    public enum Status {
        ACTIVATED,
        STOPPED,
        COOLDOWN
    }
}
