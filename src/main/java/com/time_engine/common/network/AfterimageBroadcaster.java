package com.time_engine.common.network;

import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AfterimageBroadcaster {
    private AfterimageBroadcaster() {}

    public static void tick(MinecraftServer server) {
        int serverTick = server.getTickCount();
        if (!shouldBroadcast(serverTick)) {
            return;
        }

        for (TemporalSession session : TemporalSessionManager.getInstance().getActiveSessions()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerPlayerId());
            if (owner == null) {
                continue;
            }
            int recipientCount = broadcastAnchor(owner, session, serverTick);
            logBroadcast(session, serverTick, recipientCount);
        }
    }

    private static int broadcastAnchor(
            ServerPlayer owner, TemporalSession session, int serverTick) {
        double observerRadius = TimeEngineConfig.afterimageObserverRadius();
        double observerRadiusSquared = observerRadius * observerRadius;
        AfterimagePayload payload =
                new AfterimagePayload(
                        session.sessionId(),
                        serverTick,
                        owner.level().dimension().location(),
                        TimeEngineConfig.afterimageLifetimeTicks(),
                        TemporalEntityRenderState.fromEntity(owner));

        int recipientCount = 0;
        for (ServerPlayer observer : owner.serverLevel().players()) {
            if (!shouldReceive(owner, observer, observerRadiusSquared)) {
                continue;
            }
            PacketDistributor.sendToPlayer(observer, payload);
            recipientCount++;
        }
        return recipientCount;
    }

    private static boolean shouldBroadcast(int serverTick) {
        int interval = TimeEngineConfig.afterimageIntervalTicks();
        return serverTick % interval == 0;
    }

    private static boolean shouldReceive(
            ServerPlayer owner, ServerPlayer observer, double observerRadiusSquared) {
        if (owner == observer) {
            return false;
        }
        return owner.distanceToSqr(observer) <= observerRadiusSquared;
    }

    private static void logBroadcast(TemporalSession session, int serverTick, int recipientCount) {
        if (serverTick % 100 != 0) {
            return;
        }
        ModLog.diagnostic(
                "Sent afterimage anchor for session {} at server tick {} to {} observers",
                session.sessionId(),
                serverTick,
                recipientCount);
    }
}
