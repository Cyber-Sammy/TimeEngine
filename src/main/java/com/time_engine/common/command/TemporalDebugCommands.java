package com.time_engine.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.time_engine.common.intercept.TemporalInterceptManager;
import com.time_engine.common.network.ModNetworking;
import com.time_engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.common.policy.TemporalPolicyResolver;
import com.time_engine.common.policy.TemporalPolicyResolver.ReloadStats;
import com.time_engine.common.policy.TemporalPolicyResolver.ResolvedPolicy;
import com.time_engine.common.snapshot.SnapshotManager;
import com.time_engine.common.temporal.TemporalLayerRelation;
import com.time_engine.common.temporal.TemporalScaleResolver;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TemporalConfigService;
import com.time_engine.util.ModLog;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

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
        root.then(
                Commands.literal("intercepts")
                        .executes(context -> showIntercepts(context.getSource())));
        root.then(
                Commands.literal("relation")
                        .then(
                                Commands.argument("target", EntityArgument.entity())
                                        .executes(
                                                context ->
                                                        showRelation(
                                                                context.getSource(),
                                                                EntityArgument.getEntity(
                                                                        context, "target")))));
        root.then(createPoliciesCommand());
        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createPoliciesCommand() {
        LiteralArgumentBuilder<CommandSourceStack> policies =
                Commands.literal("policies")
                        .executes(context -> showPolicyStats(context.getSource()));
        policies.then(
                Commands.literal("reload")
                        .executes(context -> reloadPolicies(context.getSource())));
        policies.then(
                Commands.literal("entity")
                        .then(
                                Commands.argument("target", EntityArgument.entity())
                                        .executes(
                                                context ->
                                                        showEntityPolicy(
                                                                context.getSource(),
                                                                EntityArgument.getEntity(
                                                                        context, "target")))));
        policies.then(
                Commands.literal("block")
                        .then(
                                Commands.argument("position", BlockPosArgument.blockPos())
                                        .executes(
                                                context ->
                                                        showBlockPolicy(
                                                                context.getSource(),
                                                                BlockPosArgument.getLoadedBlockPos(
                                                                        context, "position")))));
        return policies;
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
        SnapshotManager.TrackingDiagnostics trackingDiagnostics =
                SnapshotManager.getInstance()
                        .getTrackingDiagnostics(activeSession.sessionId())
                        .orElse(new SnapshotManager.TrackingDiagnostics(0, 0, false));
        sendSuccess(
                source,
                "Time Engine: active, currentTick=%d, perceivedTick=%.3f, startTick=%d, endTick=%d, scale=%.3f, activeSessions=%d, admitted=%d, newlyAdmitted=%d, trackingCapReached=%s",
                currentTick,
                perceivedTick,
                activeSession.startTick(),
                activeSession.endTick(),
                activeSession.timeScale(),
                manager.getActiveSessions().size(),
                trackingDiagnostics.admittedEntities(),
                trackingDiagnostics.newlyAdmittedEntities(),
                trackingDiagnostics.capReached());
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

    private static int showIntercepts(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<TemporalInterceptManager.InterceptStats> stats =
                TemporalInterceptManager.getInstance().getStats(player);
        if (stats.isEmpty()) {
            source.sendFailure(Component.literal("Time Engine: no active temporal session"));
            return 0;
        }

        TemporalInterceptManager.InterceptStats interceptStats = stats.orElseThrow();
        sendSuccess(
                source,
                "Time Engine intercepts: trackedBlocks=%d, interceptedTargets=%d",
                interceptStats.trackedBlocks(),
                interceptStats.interceptedTargets());
        return 1;
    }

    private static int showRelation(CommandSourceStack source, Entity target)
            throws CommandSyntaxException {
        ServerPlayer observer = source.getPlayerOrException();
        TemporalScaleResolver scaleResolver = TemporalScaleResolver.server();
        double observerScale = scaleResolver.effectiveScale(observer);
        double targetScale = scaleResolver.effectiveScale(target);
        TemporalLayerRelation relation = TemporalLayerRelation.compare(observerScale, targetScale);
        double perceivedTick = relativePerceivedTick(source, observer, target);
        boolean attackableGhost = relation.allowsAttackableGhost() && isCombatAllowed(target);

        sendSuccess(
                source,
                "Time Engine relation to %s: observerScale=%.3f, targetScale=%.3f, relation=%s, perceivedTick=%.3f, attackableGhost=%s",
                target.getName().getString(),
                observerScale,
                targetScale,
                relation.kind().name().toLowerCase(Locale.ROOT),
                perceivedTick,
                attackableGhost);
        return 1;
    }

    private static int showPolicyStats(CommandSourceStack source) {
        ReloadStats stats = TemporalPolicyResolver.getInstance().stats();
        sendSuccess(
                source,
                "Time Engine policies: loaded=%d, rejected=%d, generation=%d",
                stats.loadedPolicies(),
                stats.rejectedPolicies(),
                stats.generation());
        return 1;
    }

    private static int reloadPolicies(CommandSourceStack source) {
        var server = source.getServer();
        source.sendSuccess(() -> Component.literal("Time Engine: reloading datapacks"), false);
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .whenComplete(
                        (ignored, error) ->
                                server.execute(() -> reportPolicyReload(source, error)));
        return 1;
    }

    private static void reportPolicyReload(CommandSourceStack source, Throwable error) {
        if (error != null) {
            ModLog.error("Datapack reload requested by Time Engine failed", error);
            source.sendFailure(Component.literal("Time Engine: datapack reload failed"));
            return;
        }
        ReloadStats stats = TemporalPolicyResolver.getInstance().stats();
        sendSuccess(
                source,
                "Time Engine policies reloaded: loaded=%d, rejected=%d",
                stats.loadedPolicies(),
                stats.rejectedPolicies());
    }

    private static int showEntityPolicy(CommandSourceStack source, Entity target) {
        TemporalPolicyResolver resolver = TemporalPolicyResolver.getInstance();
        ResolvedPolicy snapshot =
                resolver.resolveEntity(
                        target, Operation.SNAPSHOT, TemporalPolicyDefaults.snapshot(target));
        ResolvedPolicy combat =
                resolver.resolveEntity(
                        target,
                        Operation.PHANTOM_COMBAT,
                        TemporalPolicyDefaults.phantomCombat(target));
        ResolvedPolicy intercept =
                resolver.resolveEntity(
                        target,
                        Operation.TEMPORAL_INTERCEPT,
                        TemporalPolicyDefaults.interceptEntity(target));
        sendSuccess(
                source,
                "Time Engine entity policy for %s: snapshot=%s, combat=%s, intercept=%s",
                target.getName().getString(),
                describe(snapshot),
                describe(combat),
                describe(intercept));
        return 1;
    }

    private static int showBlockPolicy(
            CommandSourceStack source, net.minecraft.core.BlockPos position) {
        BlockState blockState = source.getLevel().getBlockState(position);
        TemporalPolicyResolver resolver = TemporalPolicyResolver.getInstance();
        ResolvedPolicy intercept =
                resolver.resolveBlock(
                        blockState,
                        Operation.TEMPORAL_INTERCEPT,
                        TemporalPolicyDefaults.interceptBlock());
        ResolvedPolicy interaction =
                resolver.resolveBlock(
                        blockState, Operation.INTERACTION, TemporalPolicyDefaults.interaction());
        sendSuccess(
                source,
                "Time Engine block policy at %s: intercept=%s, interaction=%s",
                position.toShortString(),
                describe(intercept),
                describe(interaction));
        return 1;
    }

    private static String describe(ResolvedPolicy policy) {
        String source = policy.policyId().map(Object::toString).orElse("fallback");
        return policy.decision().name().toLowerCase(Locale.ROOT) + "[" + source + "]";
    }

    private static double relativePerceivedTick(
            CommandSourceStack source, ServerPlayer observer, Entity target) {
        int currentTick = source.getServer().getTickCount();
        TemporalScaleResolver scaleResolver = TemporalScaleResolver.server();
        return TemporalSessionManager.getInstance()
                .getSession(observer)
                .map(session -> scaleResolver.relativePerceivedTick(session, target, currentTick))
                .orElse((double) currentTick);
    }

    private static boolean isCombatAllowed(Entity target) {
        return TemporalPolicyResolver.getInstance()
                        .resolveEntity(
                                target,
                                Operation.PHANTOM_COMBAT,
                                TemporalPolicyDefaults.phantomCombat(target))
                        .decision()
                == Decision.ALLOW;
    }

    private static void sendSuccess(CommandSourceStack source, String format, Object... arguments) {
        Component message = Component.literal(String.format(Locale.ROOT, format, arguments));
        source.sendSuccess(() -> message, false);
    }

    private static boolean canUseCommands(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        return TemporalConfigService.canEdit(player);
    }
}
