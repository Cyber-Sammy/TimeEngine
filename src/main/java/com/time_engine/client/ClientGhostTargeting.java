package com.time_engine.client;

import com.time_engine.client.ClientGhostState.RenderedGhostFrame;
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
        GhostEntityState nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (GhostEntityState state : frame.entities()) {
            OptionalDouble distance =
                    PhantomHitDetector.rayIntersectionDistance(
                            origin, direction, reach, state.boundingBox());
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
        if (nearest == null) {
            return false;
        }

        PacketDistributor.sendToServer(
                new PhantomHitRequestPayload(nearest.entityId(), frame.perceivedTick()));
        return true;
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
