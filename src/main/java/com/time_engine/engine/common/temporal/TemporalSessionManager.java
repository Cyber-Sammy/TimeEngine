package com.time_engine.engine.common.temporal;

import com.time_engine.engine.config.TemporalConfigService;
import com.time_engine.engine.config.TemporalSessionSettings;
import com.time_engine.engine.config.TimeEngineConfig;
import com.time_engine.engine.util.ModLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalSessionManager {
    private static final TemporalSessionManager INSTANCE = new TemporalSessionManager();

    private final Map<UUID, TemporalSession> sessionsByOwner = new HashMap<>();
    private final Map<UUID, Integer> cooldownEndTicks = new HashMap<>();
    private final Map<UUID, List<TemporalScaleSegment>> scaleSegmentsByOwner = new HashMap<>();
    private final Collection<TemporalSession> activeSessions =
            Collections.unmodifiableCollection(sessionsByOwner.values());

    private TemporalSessionManager() {}

    public static TemporalSessionManager getInstance() {
        return INSTANCE;
    }

    public boolean startSession(ServerPlayer player) {
        int currentTick = player.getServer().getTickCount();
        UUID playerId = player.getUUID();

        if (isActive(player)) {
            return false;
        }
        if (getCooldownTicksRemaining(player, currentTick) > 0) {
            return false;
        }

        TemporalSessionSettings settings = TemporalConfigService.sessionSettings(player);
        TemporalSession session = createSession(playerId, currentTick, settings);
        warnIfSnapshotHistoryIsTooShort(session);
        sessionsByOwner.put(playerId, session);
        recordScaleSegment(session);
        ModLog.diagnostic(
                "Started temporal session {} for player {} at tick {} (duration={}, scale={}, radius={})",
                session.sessionId(),
                playerId,
                currentTick,
                session.durationTicks(),
                session.timeScale(),
                session.radius());
        return true;
    }

    public boolean stopSession(ServerPlayer player) {
        return stopSession(player.getUUID(), player.getServer().getTickCount(), true);
    }

    public Optional<TemporalSession> getSession(ServerPlayer player) {
        return getSession(player.getUUID());
    }

    public Optional<TemporalSession> getSession(UUID ownerPlayerId) {
        return Optional.ofNullable(sessionsByOwner.get(ownerPlayerId))
                .filter(TemporalSession::active);
    }

    public boolean isActive(ServerPlayer player) {
        return getSession(player).isPresent();
    }

    public Collection<TemporalSession> getActiveSessions() {
        return activeSessions;
    }

    public List<TemporalScaleSegment> getScaleSegments(
            UUID ownerPlayerId, int fromTick, int toTick) {
        List<TemporalScaleSegment> segments = scaleSegmentsByOwner.get(ownerPlayerId);
        if (segments == null) {
            return List.of();
        }
        return segments.stream().filter(segment -> segment.overlaps(fromTick, toTick)).toList();
    }

    public int getCooldownTicksRemaining(ServerPlayer player) {
        return getCooldownTicksRemaining(player, player.getServer().getTickCount());
    }

    public int getCooldownEndTick(ServerPlayer player) {
        return cooldownEndTicks.getOrDefault(player.getUUID(), 0);
    }

    public Collection<ServerPlayer> tick(MinecraftServer server) {
        int currentTick = server.getTickCount();
        Collection<ServerPlayer> expiredSessionOwners = new ArrayList<>();
        Iterator<Map.Entry<UUID, TemporalSession>> iterator = sessionsByOwner.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, TemporalSession> entry = iterator.next();
            TemporalSession session = entry.getValue();

            if (!session.active()) {
                iterator.remove();
                continue;
            }

            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) {
                expireSession(entry, session, null, currentTick);
                iterator.remove();
                continue;
            }
            if (session.isExpired(currentTick)) {
                expireSession(entry, session, owner, currentTick);
                iterator.remove();
                expiredSessionOwners.add(owner);
            }
        }

        cooldownEndTicks.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
        pruneScaleSegments(currentTick);
        return expiredSessionOwners;
    }

    public double getPerceivedTick(TemporalSession session, int currentServerTick) {
        int elapsedTicks = Math.max(0, currentServerTick - session.startTick());
        return session.startTick() + elapsedTicks * (double) session.timeScale();
    }

    public void clear() {
        ModLog.diagnostic(
                "Clearing {} temporal sessions and {} cooldown entries",
                sessionsByOwner.size(),
                cooldownEndTicks.size());
        sessionsByOwner.clear();
        cooldownEndTicks.clear();
        scaleSegmentsByOwner.clear();
    }

    private boolean stopSession(UUID ownerId, int currentTick, boolean applyCooldown) {
        TemporalSession session = sessionsByOwner.remove(ownerId);
        if (session == null) {
            return false;
        }
        if (!session.active()) {
            return false;
        }

        session.deactivate();
        closeScaleSegment(ownerId, currentTick);
        if (applyCooldown) {
            cooldownEndTicks.put(ownerId, currentTick + session.cooldownTicks());
        }
        ModLog.diagnostic(
                "Stopped temporal session {} at tick {}", session.sessionId(), currentTick);
        return true;
    }

    private int getCooldownTicksRemaining(ServerPlayer player, int currentTick) {
        return Math.max(
                0, cooldownEndTicks.getOrDefault(player.getUUID(), currentTick) - currentTick);
    }

    private void expireSession(
            Map.Entry<UUID, TemporalSession> entry,
            TemporalSession session,
            ServerPlayer owner,
            int currentTick) {
        session.deactivate();
        closeScaleSegment(entry.getKey(), currentTick);
        cooldownEndTicks.put(entry.getKey(), currentTick + session.cooldownTicks());
        if (owner != null) {
            owner.displayClientMessage(
                    Component.translatable("message.time_engine.temporal.expired"), true);
        }
        ModLog.diagnostic("Ended temporal session {} at tick {}", session.sessionId(), currentTick);
    }

    private static void warnIfSnapshotHistoryIsTooShort(TemporalSession session) {
        int configuredHistory = TimeEngineConfig.snapshotHistoryTicks();
        int minimumHistory =
                session.minimumSnapshotHistoryTicks(
                        TemporalConstants.SNAPSHOT_HISTORY_SAFETY_MARGIN_TICKS);
        if (configuredHistory < minimumHistory) {
            ModLog.warn(
                    "Starting temporal session {} with {} ticks of snapshot history; at least {} ticks are recommended",
                    session.sessionId(),
                    configuredHistory,
                    minimumHistory);
        }
    }

    private static TemporalSession createSession(
            UUID playerId, int currentTick, TemporalSessionSettings settings) {
        return new TemporalSession(
                UUID.randomUUID(),
                playerId,
                currentTick,
                settings.durationTicks(),
                settings.cooldownTicks(),
                settings.timeScale(),
                settings.radius());
    }

    private void recordScaleSegment(TemporalSession session) {
        scaleSegmentsByOwner
                .computeIfAbsent(session.ownerPlayerId(), ignored -> new ArrayList<>())
                .add(
                        new TemporalScaleSegment(
                                session.startTick(), session.endTick(), session.timeScale()));
    }

    private void closeScaleSegment(UUID ownerId, int currentTick) {
        List<TemporalScaleSegment> segments = scaleSegmentsByOwner.get(ownerId);
        if (segments == null || segments.isEmpty()) {
            return;
        }
        segments.getLast().closeAt(currentTick);
    }

    private void pruneScaleSegments(int currentTick) {
        int oldestRetainedTick =
                currentTick
                        - TimeEngineConfig.snapshotHistoryTicks()
                        - TemporalConstants.SNAPSHOT_HISTORY_SAFETY_MARGIN_TICKS;
        scaleSegmentsByOwner
                .values()
                .forEach(
                        segments ->
                                segments.removeIf(
                                        segment -> segment.endTick() < oldestRetainedTick));
        scaleSegmentsByOwner.values().removeIf(List::isEmpty);
    }
}
