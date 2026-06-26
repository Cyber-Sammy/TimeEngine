package com.time_engine.client;

import com.time_engine.client.ClientGhostState.RenderedGhostFrame;
import com.time_engine.common.combat.PhantomHitDetector;
import com.time_engine.common.network.PhantomHitRequestPayload;
import com.time_engine.common.network.TemporalEntityRenderState;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientGhostTargeting {
    private ClientGhostTargeting() {}

    public static boolean tryAttackNearestGhost() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canTargetGhost(minecraft)) {
            return false;
        }

        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        var renderedFrame = ClientGhostState.getRenderedFrame(partialTick);
        if (renderedFrame.isEmpty()) {
            return false;
        }
        RenderedGhostFrame frame = renderedFrame.orElseThrow();

        Vec3 origin = minecraft.player.getEyePosition(partialTick);
        Vec3 direction = minecraft.player.getViewVector(partialTick);
        double reach = ClientTemporalState.phantomAttackReach();
        Optional<TemporalEntityRenderState> nearest =
                findNearestAttackableGhost(frame.entities(), origin, direction, reach);
        if (nearest.isEmpty()) {
            return false;
        }

        TemporalEntityRenderState target = nearest.orElseThrow();
        PacketDistributor.sendToServer(
                new PhantomHitRequestPayload(target.entityId(), target.perceivedTick()));
        return true;
    }

    static Optional<TemporalEntityRenderState> findNearestAttackableGhost(
            Collection<TemporalEntityRenderState> states,
            Vec3 origin,
            Vec3 direction,
            double reach) {
        TemporalEntityRenderState nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (TemporalEntityRenderState state : states) {
            OptionalDouble distance = attackDistance(state, origin, direction, reach);
            if (distance.isEmpty()) {
                continue;
            }
            double hitDistance = distance.getAsDouble();
            if (hitDistance >= nearestDistance) {
                continue;
            }
            nearest = state;
            nearestDistance = hitDistance;
        }
        return Optional.ofNullable(nearest);
    }

    private static OptionalDouble attackDistance(
            TemporalEntityRenderState state, Vec3 origin, Vec3 direction, double reach) {
        if (!state.phantomCombatAllowed()) {
            return OptionalDouble.empty();
        }
        return PhantomHitDetector.rayIntersectionDistance(
                origin, direction, reach, state.boundingBox());
    }

    private static boolean canTargetGhost(Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }
        if (minecraft.level == null) {
            return false;
        }
        if (minecraft.screen != null) {
            return false;
        }
        return ClientTemporalState.isActive();
    }
}
