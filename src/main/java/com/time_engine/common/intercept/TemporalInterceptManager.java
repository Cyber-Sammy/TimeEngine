package com.time_engine.common.intercept;

import com.time_engine.common.policy.TemporalPolicy.Decision;
import com.time_engine.common.policy.TemporalPolicy.Operation;
import com.time_engine.common.policy.TemporalPolicyDefaults;
import com.time_engine.common.policy.TemporalPolicyResolver;
import com.time_engine.common.snapshot.EntitySnapshot;
import com.time_engine.common.snapshot.SnapshotManager;
import com.time_engine.common.temporal.TemporalSession;
import com.time_engine.common.temporal.TemporalSessionManager;
import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
        PlacedBlockRecord placedBlock = record.orElseThrow();
        if (!shouldRecordBlock(placedBlock)) {
            return;
        }

        TemporalInterceptSessionState state =
                statesBySession.computeIfAbsent(
                        session.sessionId(),
                        ignored -> new TemporalInterceptSessionState(serverTick));
        state.add(placedBlock, TimeEngineConfig.maxTemporalBlocksPerSession());
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

    public Optional<EntitySnapshot> getSplicedSnapshot(
            UUID sessionId, UUID targetId, double perceivedTick) {
        TemporalInterceptSessionState state = statesBySession.get(sessionId);
        if (state == null) {
            return Optional.empty();
        }
        return state.splice(targetId, perceivedTick)
                .map(splice -> resolveSplicedSnapshot(targetId, perceivedTick, splice));
    }

    public boolean isInteractionLocked(ServerLevel level, BlockPos position) {
        for (TemporalInterceptSessionState state : statesBySession.values()) {
            for (TemporalInterceptSessionState.TrackedBlock block : state.blocks()) {
                if (!matchesBlock(level, position, block.record())) {
                    continue;
                }
                if (isInteractionLocked(block.record().blockState())) {
                    return true;
                }
            }
        }
        return false;
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
        TemporalInterceptSessionState state =
                statesBySession.computeIfAbsent(
                        session.sessionId(),
                        ignored -> new TemporalInterceptSessionState(serverTick));
        state.trim(TimeEngineConfig.maxTemporalBlocksPerSession());
        state.removeInvalid(owner.serverLevel());

        OptionalInt previousServerTick = state.advance(serverTick);
        if (previousServerTick.isEmpty()) {
            return;
        }
        if (state.isEmpty()) {
            return;
        }

        TemporalInterceptResolver.evaluate(
                owner, session, state, previousServerTick.getAsInt(), serverTick);
    }

    private static boolean isWithinSessionRadius(
            ServerPlayer player, BlockPos position, double radius) {
        double radiusSquared = radius * radius;
        return player.position().distanceToSqr(Vec3.atCenterOf(position)) <= radiusSquared;
    }

    private static EntitySnapshot resolveSplicedSnapshot(
            UUID targetId,
            double perceivedTick,
            TemporalInterceptSessionState.TimelineSplice splice) {
        double mappedTick = splice.mappedServerTick(perceivedTick);
        return SnapshotManager.getInstance()
                .getInterpolatedSnapshot(targetId, mappedTick)
                .orElse(splice.fallbackSnapshot());
    }

    private static boolean shouldRecordBlock(PlacedBlockRecord record) {
        BlockState blockState = record.blockState();
        if (isInteractionLocked(blockState)) {
            return true;
        }
        if (record.collisionBoxes().isEmpty()) {
            return false;
        }
        TemporalPolicyResolver resolver = TemporalPolicyResolver.getInstance();
        Decision intercept =
                resolver.resolveBlock(
                                blockState,
                                Operation.TEMPORAL_INTERCEPT,
                                TemporalPolicyDefaults.interceptBlock())
                        .decision();
        if (intercept == Decision.ALLOW) {
            return true;
        }
        return false;
    }

    private static boolean isInteractionLocked(BlockState blockState) {
        return TemporalPolicyResolver.getInstance()
                        .resolveBlock(
                                blockState,
                                Operation.INTERACTION,
                                TemporalPolicyDefaults.interaction())
                        .decision()
                == Decision.LOCK_INTERACTION;
    }

    private static boolean matchesBlock(
            ServerLevel level, BlockPos position, PlacedBlockRecord record) {
        if (!record.dimension().equals(level.dimension())) {
            return false;
        }
        if (!record.position().equals(position)) {
            return false;
        }
        return record.stillExists(level);
    }

    public record InterceptStats(int trackedBlocks, int interceptedTargets) {}
}
