package com.time_engine.engine.common.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.time_engine.engine.common.causal.CausalLink;
import com.time_engine.engine.common.causal.CausalLinkDiagnostics;
import com.time_engine.engine.common.causal.CausalLinkManager;
import com.time_engine.engine.common.causal.CausalLinkRuntimeService;
import com.time_engine.engine.common.causal.CausalLinkState;
import com.time_engine.engine.common.causal.CausalLinkView;
import com.time_engine.engine.common.causal.CausalPhantomFrame;
import com.time_engine.engine.common.causal.CausalTargetSelectionRules;
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
                Commands.literal("force")
                        .then(autoForceState("soft", CausalLinkState.SOFT_LOCK))
                        .then(autoForceState("hard", CausalLinkState.HARD_LOCK))
                        .then(autoForceState("broken", CausalLinkState.BROKEN))
                        .then(autoForceState("expired", CausalLinkState.EXPIRED)));
        causal.then(
                Commands.literal("clear")
                        .executes(context -> clearForcedStateForParticipant(context.getSource())));
        causal.then(
                Commands.argument("player", EntityArgument.player())
                        .executes(
                                context ->
                                        showLink(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")))
                        .then(
                                Commands.literal("force")
                                        .then(forceState("soft", CausalLinkState.SOFT_LOCK))
                                        .then(forceState("hard", CausalLinkState.HARD_LOCK))
                                        .then(forceState("broken", CausalLinkState.BROKEN))
                                        .then(forceState("expired", CausalLinkState.EXPIRED)))
                        .then(
                                Commands.literal("clear")
                                        .executes(
                                                context ->
                                                        clearForcedState(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(
                                                                        context, "player")))));
        return causal;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> autoForceState(
            String name, CausalLinkState state) {
        return Commands.literal(name)
                .executes(context -> forceStateForParticipant(context.getSource(), state));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> forceState(
            String name, CausalLinkState state) {
        return Commands.literal(name)
                .executes(
                        context ->
                                forceState(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        state))
                .then(
                        Commands.argument("target", EntityArgument.entity())
                                .executes(
                                        context ->
                                                forceState(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        EntityArgument.getEntity(context, "target"),
                                                        state)));
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

    private static int forceStateForParticipant(CommandSourceStack source, CausalLinkState state)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer participant = source.getPlayerOrException();
        Optional<ParticipantLink> participantLink = linkForParticipant(participant);
        if (participantLink.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "Time Engine causal: no causal link involving "
                                    + participant.getName().getString()
                                    + "; use /timeengine causal <owner> force <state> <target> to create one manually"));
            return 0;
        }

        ParticipantLink resolvedLink = participantLink.orElseThrow();
        CausalLinkManager linkManager = CausalLinkRuntimeService.getInstance().linkManager();
        int serverTick = source.getServer().getTickCount();
        int hardLockTicks = CausalTargetSelectionRules.DEFAULT.hardLockTicks();
        CausalLink forced =
                linkManager
                        .forceDebugState(resolvedLink.ownerId(), state, serverTick, hardLockTicks)
                        .orElseThrow();
        TemporalDebugCommands.sendSuccess(
                source,
                "Time Engine causal: forced/frozen detected link %s -> %s to %s; use /timeengine causal clear to resume runtime updates and target switching",
                resolvedLink.ownerId(),
                forced.targetId(),
                forced.state());
        return 1;
    }

    private static int forceState(
            CommandSourceStack source, ServerPlayer owner, CausalLinkState state) {
        CausalLinkManager linkManager = CausalLinkRuntimeService.getInstance().linkManager();
        int serverTick = source.getServer().getTickCount();
        int hardLockTicks = CausalTargetSelectionRules.DEFAULT.hardLockTicks();
        Optional<CausalLink> forced =
                linkManager.forceDebugState(owner.getUUID(), state, serverTick, hardLockTicks);
        if (forced.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "Time Engine causal: no causal link to force for "
                                    + owner.getName().getString()
                                    + "; use /timeengine causal "
                                    + owner.getGameProfile().getName()
                                    + " force <soft|hard|broken|expired> <target>"));
            return 0;
        }

        TemporalDebugCommands.sendSuccess(
                source,
                "Time Engine causal: forced/frozen %s link to %s; use /timeengine causal %s clear to resume runtime updates and target switching",
                owner.getName().getString(),
                forced.orElseThrow().state(),
                owner.getGameProfile().getName());
        return 1;
    }

    private static int forceState(
            CommandSourceStack source, ServerPlayer owner, Entity target, CausalLinkState state) {
        if (target.getUUID().equals(owner.getUUID())) {
            source.sendFailure(Component.literal("Time Engine causal: target must not be owner"));
            return 0;
        }

        CausalLinkManager linkManager = CausalLinkRuntimeService.getInstance().linkManager();
        int serverTick = source.getServer().getTickCount();
        int hardLockTicks = CausalTargetSelectionRules.DEFAULT.hardLockTicks();
        CausalLink forced =
                linkManager.forceDebugLink(
                        owner.getUUID(),
                        target.getUUID(),
                        state,
                        serverTick,
                        hardLockTicks,
                        CausalPhantomFrame.capture(target, serverTick),
                        CausalPhantomFrame.capture(owner, serverTick));

        TemporalDebugCommands.sendSuccess(
                source,
                "Time Engine causal: forced/frozen %s -> %s to %s; use /timeengine causal %s clear to resume runtime updates and target switching",
                owner.getName().getString(),
                target.getName().getString(),
                forced.state(),
                owner.getGameProfile().getName());
        return 1;
    }

    private static int clearForcedStateForParticipant(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer participant = source.getPlayerOrException();
        Optional<ParticipantLink> participantLink = linkForParticipant(participant);
        if (participantLink.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "Time Engine causal: no causal link involving "
                                    + participant.getName().getString()));
            return 0;
        }

        CausalLinkRuntimeService.getInstance()
                .linkManager()
                .clearDebugState(participantLink.orElseThrow().ownerId());
        TemporalDebugCommands.sendSuccess(
                source,
                "Time Engine causal: cleared forced state for detected link; runtime updates will resume");
        return 1;
    }

    private static int clearForcedState(CommandSourceStack source, ServerPlayer owner) {
        CausalLinkRuntimeService.getInstance().linkManager().clearDebugState(owner.getUUID());
        TemporalDebugCommands.sendSuccess(
                source,
                "Time Engine causal: cleared forced state for %s; runtime updates will resume",
                owner.getName().getString());
        return 1;
    }

    private static Optional<ParticipantLink> linkForParticipant(ServerPlayer participant) {
        CausalLinkManager linkManager = CausalLinkRuntimeService.getInstance().linkManager();
        UUID participantId = participant.getUUID();
        Optional<CausalLink> ownerLink = linkManager.getLink(participantId);
        if (ownerLink.isPresent()) {
            return Optional.of(new ParticipantLink(participantId, ownerLink.orElseThrow()));
        }

        return linkManager.links().stream()
                .filter(link -> link.targetId().equals(participantId))
                .findFirst()
                .map(link -> new ParticipantLink(link.ownerId(), link));
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

    private record ParticipantLink(UUID ownerId, CausalLink link) {}
}
