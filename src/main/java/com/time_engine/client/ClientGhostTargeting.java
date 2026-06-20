package com.time_engine.client;

import com.time_engine.common.combat.PhantomHitDetector;
import com.time_engine.common.network.GhostFramePayload.GhostEntityState;
import com.time_engine.common.network.PhantomHitRequestPayload;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientGhostTargeting {
    private ClientGhostTargeting() {}

    public static boolean tryAttackNearestGhost() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null
                || !ClientTemporalState.isActive()) {
            return false;
        }

        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        var renderedFrame = ClientGhostState.getRenderedFrame(partialTick);
        if (renderedFrame.isEmpty()) {
            return false;
        }

        Vec3 origin = minecraft.player.getEyePosition(partialTick);
        Vec3 direction = minecraft.player.getViewVector(partialTick);
        double reach = ClientTemporalState.phantomAttackReach();
        GhostEntityState nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (GhostEntityState state : renderedFrame.orElseThrow().entities()) {
            OptionalDouble distance =
                    PhantomHitDetector.rayIntersectionDistance(
                            origin, direction, reach, state.boundingBox());
            if (distance.isPresent() && distance.getAsDouble() < nearestDistance) {
                nearest = state;
                nearestDistance = distance.getAsDouble();
            }
        }
        if (nearest == null) {
            return false;
        }

        PacketDistributor.sendToServer(
                new PhantomHitRequestPayload(
                        nearest.entityId(), renderedFrame.orElseThrow().perceivedTick()));
        return true;
    }
}
