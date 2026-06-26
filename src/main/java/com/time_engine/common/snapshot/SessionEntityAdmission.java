package com.time_engine.common.snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

final class SessionEntityAdmission {
    private final Set<UUID> admittedIds = new HashSet<>();
    private final Map<UUID, Integer> admissionTicks = new HashMap<>();
    private boolean initialScanCompleted;

    AdmissionUpdate update(
            List<UUID> candidateIds,
            Set<UUID> retainableIds,
            boolean trackNewEntities,
            int maxTrackedEntities,
            int currentTick) {
        int evicted = evictStaleEntries(retainableIds);
        int newlyAdmitted = 0;
        if (shouldAdmitCandidates(trackNewEntities)) {
            newlyAdmitted = admitCandidates(candidateIds, maxTrackedEntities, currentTick);
        }
        initialScanCompleted = true;
        return new AdmissionUpdate(
                newlyAdmitted, evicted, capReached(candidateIds, maxTrackedEntities));
    }

    Set<UUID> admittedIds() {
        return Set.copyOf(admittedIds);
    }

    boolean contains(UUID entityId) {
        return admittedIds.contains(entityId);
    }

    OptionalInt admissionTick(UUID entityId) {
        Integer admissionTick = admissionTicks.get(entityId);
        return admissionTick == null ? OptionalInt.empty() : OptionalInt.of(admissionTick);
    }

    private int evictStaleEntries(Set<UUID> retainableIds) {
        int before = admittedIds.size();
        admittedIds.removeIf(entityId -> !retainableIds.contains(entityId));
        admissionTicks.keySet().removeIf(entityId -> !admittedIds.contains(entityId));
        return before - admittedIds.size();
    }

    private boolean shouldAdmitCandidates(boolean trackNewEntities) {
        if (trackNewEntities) {
            return true;
        }
        return !initialScanCompleted;
    }

    private int admitCandidates(List<UUID> candidateIds, int maxTrackedEntities, int currentTick) {
        int newlyAdmitted = 0;
        for (UUID entityId : candidateIds) {
            if (admittedIds.size() >= maxTrackedEntities) {
                return newlyAdmitted;
            }
            if (admittedIds.add(entityId)) {
                admissionTicks.put(entityId, currentTick);
                newlyAdmitted++;
            }
        }
        return newlyAdmitted;
    }

    private boolean capReached(List<UUID> candidateIds, int maxTrackedEntities) {
        if (admittedIds.size() < maxTrackedEntities) {
            return false;
        }
        return candidateIds.stream().anyMatch(entityId -> !admittedIds.contains(entityId));
    }

    record AdmissionUpdate(int newlyAdmitted, int evicted, boolean capReached) {}
}
