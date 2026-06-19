package com.time_engine.common.snapshot;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class EntitySnapshotBuffer {
    private final UUID entityId;
    private final EntitySnapshot[] snapshots;
    private int size;
    private int latestRecordedTick = Integer.MIN_VALUE;

    public EntitySnapshotBuffer(UUID entityId, int capacity) {
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        if (capacity < 2) {
            throw new IllegalArgumentException("capacity must be at least 2");
        }
        this.snapshots = new EntitySnapshot[capacity];
    }

    public void addSnapshot(EntitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!entityId.equals(snapshot.entityId())) {
            throw new IllegalArgumentException("Snapshot entity does not match this buffer");
        }
        if (latestRecordedTick != Integer.MIN_VALUE && snapshot.serverTick() < latestRecordedTick) {
            throw new IllegalArgumentException("Snapshots must be added in server tick order");
        }

        int index = indexFor(snapshot.serverTick());
        EntitySnapshot replaced = snapshots[index];
        if (replaced == null) {
            size++;
        }
        snapshots[index] = snapshot;
        latestRecordedTick = snapshot.serverTick();
    }

    public Optional<EntitySnapshot> getSnapshotAtTick(int serverTick) {
        EntitySnapshot snapshot = snapshots[indexFor(serverTick)];
        if (snapshot == null || snapshot.serverTick() != serverTick) {
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public Optional<EntitySnapshot> getInterpolatedSnapshot(double serverTick) {
        if (!Double.isFinite(serverTick)) {
            return Optional.empty();
        }

        int lowerTick = (int) Math.floor(serverTick);
        int upperTick = (int) Math.ceil(serverTick);
        Optional<EntitySnapshot> lower = getSnapshotAtTick(lowerTick);
        if (lowerTick == upperTick) {
            return lower;
        }

        Optional<EntitySnapshot> upper = getSnapshotAtTick(upperTick);
        if (lower.isPresent() && upper.isPresent()) {
            return Optional.of(lower.get().interpolate(upper.get(), serverTick - lowerTick));
        }
        return lower.or(() -> upper);
    }

    public UUID entityId() {
        return entityId;
    }

    public int capacity() {
        return snapshots.length;
    }

    public int size() {
        return size;
    }

    public int latestRecordedTick() {
        return latestRecordedTick;
    }

    private int indexFor(int serverTick) {
        return Math.floorMod(serverTick, snapshots.length);
    }
}
