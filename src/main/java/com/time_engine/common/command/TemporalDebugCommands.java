package com.time_engine.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.time_engine.common.network.ModNetworking;
import com.time_engine.common.snapshot.SnapshotManager;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TemporalConfigService;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalDebugCommands {
    private TemporalDebugCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal("timeengine").requires(TemporalDebugCommands::canUseCommands);
        root.then(
                Commands.literal("session").executes(context -> showSession(context.getSource())));
        root.then(
                Commands.literal("snapshots")
                        .then(
                                Commands.argument("player", EntityArgument.player())
                                        .executes(
                                                context ->
                                                        showSnapshots(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(
                                                                        context, "player")))));
        root.then(Commands.literal("config").executes(context -> openConfig(context.getSource())));
        dispatcher.register(root);
    }

    private static int openConfig(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!TemporalConfigService.canEdit(player)) {
            source.sendFailure(Component.literal("Time Engine: permission level 2 is required"));
            return 0;
        }

        ModNetworking.sendConfigScreen(player, true, "Loaded server configuration");
        return 1;
    }

    private static int showSession(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TemporalSessionManager manager = TemporalSessionManager.getInstance();
        int currentTick = source.getServer().getTickCount();
        Optional<TemporalSession> session = manager.getSession(player);

        if (session.isEmpty()) {
            int cooldown = manager.getCooldownTicksRemaining(player);
            sendSuccess(
                    source,
                    "Time Engine: inactive, currentTick=%d, cooldown=%d, activeSessions=%d",
                    currentTick,
                    cooldown,
                    manager.getActiveSessions().size());
            return 1;
        }

        TemporalSession activeSession = session.get();
        double perceivedTick = manager.getPerceivedTick(activeSession, currentTick);
        sendSuccess(
                source,
                "Time Engine: active, currentTick=%d, perceivedTick=%.3f, startTick=%d, endTick=%d, scale=%.3f, activeSessions=%d",
                currentTick,
                perceivedTick,
                activeSession.startTick(),
                activeSession.endTick(),
                activeSession.timeScale(),
                manager.getActiveSessions().size());
        return 1;
    }

    private static int showSnapshots(CommandSourceStack source, ServerPlayer target)
            throws CommandSyntaxException {
        ServerPlayer viewer = source.getPlayerOrException();
        TemporalSessionManager sessionManager = TemporalSessionManager.getInstance();
        SnapshotManager snapshotManager = SnapshotManager.getInstance();
        int currentTick = source.getServer().getTickCount();
        double perceivedTick =
                sessionManager
                        .getSession(viewer)
                        .map(session -> sessionManager.getPerceivedTick(session, currentTick))
                        .orElse((double) currentTick);

        Optional<SnapshotManager.BufferStats> stats =
                snapshotManager.getBufferStats(target.getUUID());
        if (stats.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "Time Engine: no snapshot buffer for " + target.getName().getString()));
            return 0;
        }

        SnapshotManager.BufferStats buffer = stats.get();
        boolean hasInterpolatedSnapshot =
                snapshotManager
                        .getInterpolatedSnapshot(target.getUUID(), perceivedTick)
                        .isPresent();
        sendSuccess(
                source,
                "Time Engine snapshots for %s: currentTick=%d, perceivedTick=%.3f, size=%d/%d, latestTick=%d, interpolated=%s",
                target.getName().getString(),
                currentTick,
                perceivedTick,
                buffer.size(),
                buffer.capacity(),
                buffer.latestSnapshotTick(),
                hasInterpolatedSnapshot);
        return 1;
    }

    private static void sendSuccess(CommandSourceStack source, String format, Object... arguments) {
        Component message = Component.literal(String.format(Locale.ROOT, format, arguments));
        source.sendSuccess(() -> message, false);
    }

    private static boolean canUseCommands(CommandSourceStack source) {
        return source.hasPermission(2)
                || source.getEntity() instanceof ServerPlayer player
                        && TemporalConfigService.canEdit(player);
    }
}
