package com.time_engine.common.intercept;

import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class TemporalInterceptManager {
    private static final TemporalInterceptManager INSTANCE = new TemporalInterceptManager();

    private final Map<UUID, TemporalInterceptSessionState> statesBySession = new HashMap<>();

    private TemporalInterceptManager() {}

    public static TemporalInterceptManager getInstance() {
        return INSTANCE;
    }

    public void recordPlacement(ServerPlayer player, BlockPos position, BlockState blockState) {
        if (!TimeEngineConfig.temporalInterceptEnabled()) {
            return;
        }

        TemporalSessionManager sessionManager = TemporalSessionManager.getInstance();
        Optional<TemporalSession> sessionResult = sessionManager.getSession(player);
        if (sessionResult.isEmpty()) {
            return;
        }

        TemporalSession session = sessionResult.orElseThrow();
        if (!isWithinSessionRadius(player, position, session.radius())) {
            return;
        }

        int serverTick = player.getServer().getTickCount();
        Optional<PlacedBlockRecord> record =
                PlacedBlockRecord.create(
                        session.sessionId(),
                        player.getUUID(),
                        player.serverLevel(),
                        position,
                        blockState,
                        serverTick);
        if (record.isEmpty()) {
            return;
        }

        double perceivedTick = sessionManager.getPerceivedTick(session, serverTick);
        TemporalInterceptSessionState state =
                statesBySession.computeIfAbsent(
                        session.sessionId(),
                        ignored -> new TemporalInterceptSessionState(perceivedTick));
        state.add(record.orElseThrow(), TimeEngineConfig.maxTemporalBlocksPerSession());
        ModLog.diagnostic(
                "Recorded temporal block {} for session {} at server tick {}",
                position,
                session.sessionId(),
                serverTick);
    }

    public void tick(MinecraftServer server) {
        if (!TimeEngineConfig.temporalInterceptEnabled()) {
            clear();
            return;
        }

        Collection<TemporalSession> activeSessions =
                TemporalSessionManager.getInstance().getActiveSessions();
        Set<UUID> activeSessionIds = new HashSet<>();
        for (TemporalSession session : activeSessions) {
            activeSessionIds.add(session.sessionId());
            tickSession(server, session);
        }
        statesBySession.keySet().removeIf(sessionId -> !activeSessionIds.contains(sessionId));
    }

    public Optional<InterceptStats> getStats(ServerPlayer player) {
        Optional<TemporalSession> session = TemporalSessionManager.getInstance().getSession(player);
        if (session.isEmpty()) {
            return Optional.empty();
        }
        TemporalInterceptSessionState state =
                statesBySession.get(session.orElseThrow().sessionId());
        if (state == null) {
            return Optional.of(new InterceptStats(0, 0));
        }
        return Optional.of(new InterceptStats(state.blockCount(), state.interceptedTargetCount()));
    }

    public void clear() {
        if (!statesBySession.isEmpty()) {
            ModLog.diagnostic(
                    "Clearing {} temporal intercept session states", statesBySession.size());
        }
        statesBySession.clear();
    }

    private void tickSession(MinecraftServer server, TemporalSession session) {
        ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerPlayerId());
        if (owner == null) {
            return;
        }

        int serverTick = server.getTickCount();
        double currentPerceivedTick =
                TemporalSessionManager.getInstance().getPerceivedTick(session, serverTick);
        TemporalInterceptSessionState state =
                statesBySession.computeIfAbsent(
                        session.sessionId(),
                        ignored -> new TemporalInterceptSessionState(currentPerceivedTick));
        state.trim(TimeEngineConfig.maxTemporalBlocksPerSession());
        state.removeInvalid(owner.serverLevel());

        OptionalDouble previousPerceivedTick = state.advance(currentPerceivedTick);
        if (previousPerceivedTick.isEmpty()) {
            return;
        }
        if (state.isEmpty()) {
            return;
        }

        TemporalInterceptResolver.evaluate(
                owner, session, state, previousPerceivedTick.getAsDouble(), currentPerceivedTick);
    }

    private static boolean isWithinSessionRadius(
            ServerPlayer player, BlockPos position, double radius) {
        double radiusSquared = radius * radius;
        return player.position().distanceToSqr(Vec3.atCenterOf(position)) <= radiusSquared;
    }

    public record InterceptStats(int trackedBlocks, int interceptedTargets) {}
}
