package com.time_engine.engine.common.network;

import com.time_engine.engine.common.causal.CausalLink;
import com.time_engine.engine.common.causal.CausalLinkRuntimeService;
import com.time_engine.engine.common.causal.CausalPhantomFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CausalLinkFrameBroadcaster {
    private CausalLinkFrameBroadcaster() {}

    public static void tick(MinecraftServer server) {
        int serverTick = server.getTickCount();
        CausalLinkRuntimeService.getInstance().linkManager().links().stream()
                .filter(CausalLink::active)
                .map(link -> deliveriesFor(server, link, serverTick))
                .flatMap(List::stream)
                .forEach(
                        delivery ->
                                PacketDistributor.sendToPlayer(
                                        delivery.target(), delivery.payload()));
    }

    private static List<Delivery> deliveriesFor(
            MinecraftServer server, CausalLink link, int serverTick) {
        ServerPlayer owner = server.getPlayerList().getPlayer(link.ownerId());
        if (owner == null) {
            return List.of();
        }

        Entity target = owner.serverLevel().getEntity(link.targetId());
        CausalLinkFramePayload payload =
                payloadFor(
                        link,
                        owner.level().dimension().location(),
                        targetAnchor(target, link),
                        serverTick);
        List<Delivery> deliveries = new ArrayList<>();
        targetPlayer(target, owner)
                .ifPresent(player -> deliveries.add(new Delivery(player, payload)));
        deliveries.add(new Delivery(owner, payload));
        return deliveries;
    }

    static CausalLinkFramePayload payloadFor(
            CausalLink link, ResourceLocation dimension, Vec3 targetAnchor, int serverTick) {
        return new CausalLinkFramePayload(
                link.ownerId(),
                link.targetId(),
                link.state(),
                serverTick,
                link.progress(),
                dimension,
                pursuitRenderState(link, targetAnchor),
                renderState(link.latestFrame()),
                renderState(link.originOwnerFrame()),
                renderState(link.originTargetFrame()));
    }

    private static Vec3 targetAnchor(Entity target, CausalLink link) {
        if (target == null) {
            return link.latestFrame().stableAnchor();
        }
        return target.position();
    }

    private static Optional<ServerPlayer> targetPlayer(Entity target, ServerPlayer owner) {
        if (!(target instanceof ServerPlayer targetPlayer)) {
            return Optional.empty();
        }
        if (targetPlayer.getUUID().equals(owner.getUUID())) {
            return Optional.empty();
        }
        if (targetPlayer.level().dimension() != owner.level().dimension()) {
            return Optional.empty();
        }
        return Optional.of(targetPlayer);
    }

    static TemporalEntityRenderState pursuitRenderState(CausalLink link, Vec3 targetAnchor) {
        CausalPhantomFrame ownerFrame = link.ownerFrame();
        Vec3 anchor = pursuitAnchor(link, targetAnchor);
        Vec3 offset = anchor.subtract(ownerFrame.stableAnchor());
        return new TemporalEntityRenderState(
                ownerFrame.entityId(),
                anchor,
                ownerFrame.pose().yRot(),
                ownerFrame.pose().xRot(),
                ownerFrame.pose().pose(),
                translate(ownerFrame.authoritativeBounds(), offset),
                ownerFrame.serverTick(),
                true);
    }

    private static Vec3 pursuitAnchor(CausalLink link, Vec3 targetAnchor) {
        double progress = Mth.clamp(link.progress(), 0.0D, 1.0D);
        return link.originOwnerFrame().stableAnchor().lerp(targetAnchor, progress);
    }

    private static TemporalEntityRenderState renderState(CausalPhantomFrame frame) {
        return new TemporalEntityRenderState(
                frame.entityId(),
                frame.stableAnchor(),
                frame.pose().yRot(),
                frame.pose().xRot(),
                frame.pose().pose(),
                frame.authoritativeBounds(),
                frame.serverTick(),
                true);
    }

    private static AABB translate(AABB bounds, Vec3 offset) {
        return bounds.move(offset.x, offset.y, offset.z);
    }

    private record Delivery(ServerPlayer target, CausalLinkFramePayload payload) {}
}
