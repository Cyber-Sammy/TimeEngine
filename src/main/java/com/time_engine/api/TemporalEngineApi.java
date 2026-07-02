package com.time_engine.api;

import com.time_engine.engine.common.temporal.TemporalActivationService;
import com.time_engine.engine.common.temporal.TemporalActivationService.ActivationOutcome;
import com.time_engine.engine.common.temporal.TemporalSession;
import com.time_engine.engine.common.temporal.TemporalSessionManager;
import com.time_engine.engine.config.TemporalConfigService;
import com.time_engine.engine.config.TemporalSessionSettings;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalEngineApi {
    private TemporalEngineApi() {}

    public static TemporalActivationResult activate(ServerPlayer player) {
        return activate(player, toOptions(TemporalConfigService.sessionSettings(player)));
    }

    public static TemporalActivationResult activate(
            ServerPlayer player, TemporalSessionOptions options) {
        return toActivationResult(
                TemporalActivationService.activate(player, toEngineSettings(options)));
    }

    public static TemporalActivationResult stop(ServerPlayer player) {
        return toActivationResult(TemporalActivationService.stop(player));
    }

    public static TemporalActivationResult toggle(ServerPlayer player) {
        return toActivationResult(TemporalActivationService.toggle(player));
    }

    public static boolean toggleTemporalSession(ServerPlayer player) {
        return toggle(player).success();
    }

    public static Optional<TemporalSessionState> sessionState(ServerPlayer player) {
        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        return manager.getSession(player)
                .map(
                        session ->
                                toSessionState(
                                        player,
                                        session,
                                        manager.getPerceivedTick(
                                                session, player.getServer().getTickCount())));
    }

    private static TemporalSessionSettings toEngineSettings(TemporalSessionOptions options) {
        return new TemporalSessionSettings(
                options.durationTicks(),
                options.cooldownTicks(),
                options.timeScale(),
                options.radius());
    }

    private static TemporalSessionOptions toOptions(TemporalSessionSettings settings) {
        return new TemporalSessionOptions(
                settings.durationTicks(),
                settings.cooldownTicks(),
                settings.timeScale(),
                settings.radius());
    }

    private static TemporalActivationResult toActivationResult(ActivationOutcome outcome) {
        return switch (outcome.status()) {
            case ACTIVATED -> TemporalActivationResult.activated();
            case STOPPED -> TemporalActivationResult.stopped();
            case COOLDOWN -> TemporalActivationResult.cooldown(outcome.cooldownTicksRemaining());
        };
    }

    private static TemporalSessionState toSessionState(
            ServerPlayer player, TemporalSession session, double perceivedTick) {
        int serverTick = player.getServer().getTickCount();
        return new TemporalSessionState(
                session.sessionId(),
                session.ownerPlayerId(),
                session.active(),
                session.startTick(),
                session.durationTicks(),
                session.cooldownTicks(),
                session.timeScale(),
                session.radius(),
                serverTick,
                perceivedTick,
                Math.max(0, session.endTick() - serverTick),
                TemporalSessionManager.getInstance().getCooldownTicksRemaining(player));
    }
}
