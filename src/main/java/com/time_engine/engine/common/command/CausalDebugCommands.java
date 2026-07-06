package com.time_engine.engine.common.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.time_engine.engine.common.causal.CausalLink;
import com.time_engine.engine.common.causal.CausalLinkDiagnostics;
import com.time_engine.engine.common.causal.CausalLinkRuntimeService;
import com.time_engine.engine.common.causal.CausalLinkView;
import com.time_engine.engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.engine.common.policy.TemporalPolicyResolver;
import com.time_engine.engine.common.temporal.TemporalLayerRelation;
import com.time_engine.engine.common.temporal.TemporalScaleResolver;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

final class CausalDebugCommands {
    private CausalDebugCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> create() {
        LiteralArgumentBuilder<CommandSourceStack> causal =
                Commands.literal("causal").executes(context -> showLinks(context.getSource()));
        causal.then(
                Commands.argument("player", EntityArgument.player())
                        .executes(
                                context ->
                                        showLink(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))));
        return causal;
    }

    private static int showLinks(CommandSourceStack source) {
        int serverTick = source.getServer().getTickCount();
        var views = CausalLinkRuntimeService.getInstance().linkManager().views(serverTick);
        if (views.isEmpty()) {
            source.sendFailure(Component.literal("Time Engine causal: no causal links"));
            return 0;
        }

        TemporalDebugCommands.sendSuccess(
                source, "Time Engine causal links: count=%d", views.size());
        for (CausalLinkView view : views) {
            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "Time Engine causal: " + CausalLinkDiagnostics.summary(view)),
                    false);
        }
        return views.size();
    }

    private static int showLink(CommandSourceStack source, ServerPlayer owner) {
        Optional<CausalLink> link =
                CausalLinkRuntimeService.getInstance()
                        .linkManager()
                        .getLink(owner.getUUID())
                        .filter(CausalLink::active);
        if (link.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "Time Engine causal: no active causal link for "
                                    + owner.getName().getString()));
            return 0;
        }

        CausalLink activeLink = link.orElseThrow();
        Optional<Entity> target = findEntity(source, activeLink.targetId());
        TemporalDebugCommands.sendSuccess(
                source,
                "%s",
                CausalLinkDiagnostics.detail(
                        activeLink,
                        source.getServer().getTickCount(),
                        owner.getName().getString(),
                        targetName(target, activeLink.targetId()),
                        target.isPresent(),
                        target.map(Entity::isAlive).orElse(false),
                        target.map(entity -> relationAllows(owner, entity)).orElse(false),
                        target.map(CausalDebugCommands::isCombatAllowed).orElse(false)));
        return 1;
    }

    private static boolean relationAllows(ServerPlayer owner, Entity target) {
        TemporalScaleResolver scaleResolver = TemporalScaleResolver.server();
        return TemporalLayerRelation.compare(
                        scaleResolver.effectiveScale(owner), scaleResolver.effectiveScale(target))
                .allowsAttackableGhost();
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

    private static Optional<Entity> findEntity(CommandSourceStack source, UUID entityId) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    private static String targetName(Optional<Entity> target, UUID targetId) {
        return target.map(entity -> entity.getName().getString()).orElse(targetId.toString());
    }
}
