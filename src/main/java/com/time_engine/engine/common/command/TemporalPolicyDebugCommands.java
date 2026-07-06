package com.time_engine.engine.common.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.engine.common.policy.TemporalPolicyResolver;
import com.time_engine.engine.common.policy.TemporalPolicyResolver.ReloadStats;
import com.time_engine.engine.common.policy.TemporalPolicyResolver.ResolvedPolicy;
import com.time_engine.engine.util.ModLog;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

final class TemporalPolicyDebugCommands {
    private TemporalPolicyDebugCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> create() {
        LiteralArgumentBuilder<CommandSourceStack> policies =
                Commands.literal("policies").executes(context -> showStats(context.getSource()));
        policies.then(Commands.literal("reload").executes(context -> reload(context.getSource())));
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

    private static int showStats(CommandSourceStack source) {
        ReloadStats stats = TemporalPolicyResolver.getInstance().stats();
        TemporalDebugCommands.sendSuccess(
                source,
                "Time Engine policies: loaded=%d, rejected=%d, generation=%d",
                stats.loadedPolicies(),
                stats.rejectedPolicies(),
                stats.generation());
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        var server = source.getServer();
        source.sendSuccess(() -> Component.literal("Time Engine: reloading datapacks"), false);
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .whenComplete(
                        (ignored, error) -> server.execute(() -> reportReload(source, error)));
        return 1;
    }

    private static void reportReload(CommandSourceStack source, Throwable error) {
        if (error != null) {
            ModLog.error("Datapack reload requested by Time Engine failed", error);
            source.sendFailure(Component.literal("Time Engine: datapack reload failed"));
            return;
        }
        ReloadStats stats = TemporalPolicyResolver.getInstance().stats();
        TemporalDebugCommands.sendSuccess(
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
        TemporalDebugCommands.sendSuccess(
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
        TemporalDebugCommands.sendSuccess(
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
}
