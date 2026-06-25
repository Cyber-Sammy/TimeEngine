package com.time_engine.common.network;

import com.time_engine.common.snapshot.EntitySnapshot;
import com.time_engine.common.snapshot.SnapshotManager;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GhostFrameBroadcaster {
    private GhostFrameBroadcaster() {}

    public static void tick(MinecraftServer server) {
        int serverTick = server.getTickCount();
        if (serverTick % TimeEngineConfig.ghostFrameIntervalTicks() != 0) {
            return;
        }

        TemporalSessionManager sessionManager = TemporalSessionManager.getInstance();
        SnapshotManager snapshotManager = SnapshotManager.getInstance();
        for (TemporalSession session : sessionManager.getActiveSessions()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerPlayerId());
            if (owner == null) {
                continue;
            }

            double perceivedTick = sessionManager.getPerceivedTick(session, serverTick);
            List<EntitySnapshot> snapshots =
                    snapshotManager.getInterpolatedSnapshots(
                            owner.level().dimension(),
                            owner.position(),
                            session.radius(),
                            perceivedTick,
                            owner.getUUID(),
                            TimeEngineConfig.maxTrackedEntitiesPerSession());
            PacketDistributor.sendToPlayer(
                    owner,
                    GhostFramePayload.create(
                            session.sessionId(),
                            serverTick,
                            perceivedTick,
                            owner.level().dimension().location(),
                            snapshots,
                            owner.serverLevel()::getEntity));
            if (serverTick % 100 == 0) {
                ModLog.diagnostic(
                        "Sent ghost frame for session {} at server tick {} (perceivedTick={}, entities={})",
                        session.sessionId(),
                        serverTick,
                        perceivedTick,
                        snapshots.size());
            }
        }
    }
}
