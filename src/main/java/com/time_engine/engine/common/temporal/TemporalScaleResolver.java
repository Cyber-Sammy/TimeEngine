package com.time_engine.engine.common.temporal;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class TemporalScaleResolver {
    public static final double NORMAL_TIME_SCALE = 1.0D;

    private final TemporalSessionManager sessionManager;

    public TemporalScaleResolver(TemporalSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static TemporalScaleResolver server() {
        return new TemporalScaleResolver(TemporalSessionManager.getInstance());
    }

    public double effectiveScale(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return NORMAL_TIME_SCALE;
        }
        return sessionManager
                .getSession(player)
                .map(TemporalSession::timeScale)
                .map(Float::doubleValue)
                .orElse(NORMAL_TIME_SCALE);
    }

    public double relativePerceivedTick(
            TemporalSession observerSession, Entity target, int serverTick) {
        List<TemporalScaleSegment> targetSegments =
                target instanceof ServerPlayer player
                        ? sessionManager.getScaleSegments(
                                player.getUUID(), observerSession.startTick(), serverTick)
                        : List.of();
        return relativePerceivedTick(observerSession, targetSegments, serverTick);
    }

    public static double relativePerceivedTick(
            TemporalSession observerSession,
            List<TemporalScaleSegment> targetSegments,
            int serverTick) {
        int fromTick = observerSession.startTick();
        int toTick = Math.max(fromTick, serverTick);
        double delayTicks = 0.0D;
        int cursor = fromTick;

        List<TemporalScaleSegment> sortedSegments =
                targetSegments.stream()
                        .sorted(java.util.Comparator.comparingInt(TemporalScaleSegment::startTick))
                        .toList();

        for (TemporalScaleSegment segment : sortedSegments) {
            if (segment.endTick() <= cursor) {
                continue;
            }
            int segmentStart = Math.max(cursor, segment.startTick());
            int segmentEnd = Math.min(toTick, segment.endTick());
            if (segmentStart > cursor) {
                delayTicks +=
                        delay(cursor, segmentStart, NORMAL_TIME_SCALE, observerSession.timeScale());
            }
            if (segmentEnd > segmentStart) {
                delayTicks +=
                        delay(
                                segmentStart,
                                segmentEnd,
                                segment.scale(),
                                observerSession.timeScale());
                cursor = segmentEnd;
            }
            if (cursor >= toTick) {
                break;
            }
        }

        if (cursor < toTick) {
            delayTicks += delay(cursor, toTick, NORMAL_TIME_SCALE, observerSession.timeScale());
        }
        return clamp(toTick - delayTicks, fromTick, toTick);
    }

    private static double delay(
            int fromTick, int toTick, double targetScale, double observerScale) {
        double scaleDifference = targetScale - observerScale;
        if (scaleDifference <= 0.0D) {
            return 0.0D;
        }
        return (toTick - fromTick) * scaleDifference;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
