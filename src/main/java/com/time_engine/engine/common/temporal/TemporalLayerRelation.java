package com.time_engine.engine.common.temporal;

public record TemporalLayerRelation(
        double observerScale, double targetScale, TemporalLayerRelation.Kind kind) {
    public static final double DEFAULT_EPSILON = 0.0001D;

    public TemporalLayerRelation {
        if (!Double.isFinite(observerScale)) {
            throw new IllegalArgumentException("observerScale must be finite");
        }
        if (!Double.isFinite(targetScale)) {
            throw new IllegalArgumentException("targetScale must be finite");
        }
        if (observerScale <= 0.0D) {
            throw new IllegalArgumentException("observerScale must be positive");
        }
        if (targetScale <= 0.0D) {
            throw new IllegalArgumentException("targetScale must be positive");
        }
    }

    public static TemporalLayerRelation compare(double observerScale, double targetScale) {
        return compare(observerScale, targetScale, DEFAULT_EPSILON);
    }

    public static TemporalLayerRelation compare(
            double observerScale, double targetScale, double epsilon) {
        validateEpsilon(epsilon);
        if (Math.abs(observerScale - targetScale) <= epsilon) {
            return new TemporalLayerRelation(observerScale, targetScale, Kind.SAME_LAYER);
        }
        if (observerScale < targetScale) {
            return new TemporalLayerRelation(observerScale, targetScale, Kind.OBSERVER_FASTER);
        }
        return new TemporalLayerRelation(observerScale, targetScale, Kind.TARGET_FASTER);
    }

    public boolean allowsAttackableGhost() {
        return kind == Kind.OBSERVER_FASTER;
    }

    public double relativePerceivedTick(TemporalSession observerSession, int serverTick) {
        if (!allowsAttackableGhost()) {
            return serverTick;
        }
        int elapsedTicks = Math.max(0, serverTick - observerSession.startTick());
        double relativeDelayTicks = elapsedTicks * (targetScale - observerScale);
        return clamp(serverTick - relativeDelayTicks, observerSession.startTick(), serverTick);
    }

    private static void validateEpsilon(double epsilon) {
        if (!Double.isFinite(epsilon)) {
            throw new IllegalArgumentException("epsilon must be finite");
        }
        if (epsilon < 0.0D) {
            throw new IllegalArgumentException("epsilon must be non-negative");
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum Kind {
        SAME_LAYER,
        OBSERVER_FASTER,
        TARGET_FASTER
    }
}
