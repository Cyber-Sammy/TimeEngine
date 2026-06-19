package com.time_engine.common.temporal;

import com.time_engine.config.TimeEngineConfig;
import com.time_engine.util.ModLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TemporalSessionManager {
    private static final TemporalSessionManager INSTANCE = new TemporalSessionManager();

    private final Map<UUID, TemporalSession> sessionsByOwner = new HashMap<>();
    private final Map<UUID, Integer> cooldownEndTicks = new HashMap<>();
    private final Collection<TemporalSession> activeSessions = Collections.unmodifiableCollection(sessionsByOwner.values());

    private TemporalSessionManager() {
    }

    public static TemporalSessionManager getInstance() {
        return INSTANCE;
    }

    public boolean startSession(ServerPlayer player) {
        int currentTick = player.getServer().getTickCount();
        UUID playerId = player.getUUID();

        if (isActive(player) || getCooldownTicksRemaining(player, currentTick) > 0) {
            return false;
        }

        TemporalSession session = new TemporalSession(
                UUID.randomUUID(),
                playerId,
                currentTick,
                TimeEngineConfig.durationTicks(),
                TimeEngineConfig.timeScale(),
                TimeEngineConfig.radius()
        );
        sessionsByOwner.put(playerId, session);
        ModLog.diagnostic(
                "Started temporal session {} for player {} at tick {} (duration={}, scale={}, radius={})",
                session.sessionId(), playerId, currentTick, session.durationTicks(), session.timeScale(), session.radius()
        );
        return true;
    }

    public boolean stopSession(ServerPlayer player) {
        return stopSession(player.getUUID(), player.getServer().getTickCount(), true);
    }

    public Optional<TemporalSession> getSession(ServerPlayer player) {
        return Optional.ofNullable(sessionsByOwner.get(player.getUUID())).filter(TemporalSession::active);
    }

    public boolean isActive(ServerPlayer player) {
        return getSession(player).isPresent();
    }

    public Collection<TemporalSession> getActiveSessions() {
        return activeSessions;
    }

    public int getCooldownTicksRemaining(ServerPlayer player) {
        return getCooldownTicksRemaining(player, player.getServer().getTickCount());
    }

    public void tick(MinecraftServer server) {
        int currentTick = server.getTickCount();
        Iterator<Map.Entry<UUID, TemporalSession>> iterator = sessionsByOwner.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, TemporalSession> entry = iterator.next();
            TemporalSession session = entry.getValue();

            if (!session.active()) {
                iterator.remove();
                continue;
            }

            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null || session.isExpired(currentTick)) {
                session.deactivate();
                iterator.remove();
                cooldownEndTicks.put(entry.getKey(), currentTick + TimeEngineConfig.cooldownTicks());

                if (owner != null) {
                    owner.displayClientMessage(Component.translatable("message.time_engine.temporal.expired"), true);
                }
                ModLog.diagnostic("Ended temporal session {} at tick {}", session.sessionId(), currentTick);
            }
        }

        cooldownEndTicks.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
    }

    public double getPerceivedTick(TemporalSession session, int currentServerTick) {
        int elapsedTicks = Math.max(0, currentServerTick - session.startTick());
        return session.startTick() + elapsedTicks * (double) session.timeScale();
    }

    public void clear() {
        ModLog.diagnostic(
                "Clearing {} temporal sessions and {} cooldown entries",
                sessionsByOwner.size(), cooldownEndTicks.size()
        );
        sessionsByOwner.clear();
        cooldownEndTicks.clear();
    }

    private boolean stopSession(UUID ownerId, int currentTick, boolean applyCooldown) {
        TemporalSession session = sessionsByOwner.remove(ownerId);
        if (session == null || !session.active()) {
            return false;
        }

        session.deactivate();
        if (applyCooldown) {
            cooldownEndTicks.put(ownerId, currentTick + TimeEngineConfig.cooldownTicks());
        }
        ModLog.diagnostic("Stopped temporal session {} at tick {}", session.sessionId(), currentTick);
        return true;
    }

    private int getCooldownTicksRemaining(ServerPlayer player, int currentTick) {
        return Math.max(0, cooldownEndTicks.getOrDefault(player.getUUID(), currentTick) - currentTick);
    }
}
