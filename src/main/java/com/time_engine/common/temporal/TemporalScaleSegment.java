package com.time_engine.common.temporal;

public final class TemporalScaleSegment {
    private final int startTick;
    private int endTick;
    private final double scale;

    public TemporalScaleSegment(int startTick, int endTick, double scale) {
        if (endTick < startTick) {
            throw new IllegalArgumentException(
                    "endTick must be greater than or equal to startTick");
        }
        if (!Double.isFinite(scale)) {
            throw new IllegalArgumentException("scale must be finite");
        }
        if (scale <= 0.0D) {
            throw new IllegalArgumentException("scale must be positive");
        }

        this.startTick = startTick;
        this.endTick = endTick;
        this.scale = scale;
    }

    public int startTick() {
        return startTick;
    }

    public int endTick() {
        return endTick;
    }

    public double scale() {
        return scale;
    }

    public boolean overlaps(int fromTick, int toTick) {
        if (toTick <= startTick) {
            return false;
        }
        return fromTick < endTick;
    }

    public void closeAt(int tick) {
        endTick = Math.max(startTick, Math.min(endTick, tick));
    }
}
