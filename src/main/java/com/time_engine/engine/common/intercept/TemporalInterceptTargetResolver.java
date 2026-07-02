package com.time_engine.engine.common.intercept;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

final class TemporalInterceptTargetResolver {
    private TemporalInterceptTargetResolver() {}

    static Optional<Entity> correctionTarget(
            ServerPlayer owner, Entity candidate, Set<UUID> evaluatedTargets) {
        if (candidate == null) {
            return Optional.empty();
        }

        Entity target = rootVehicle(candidate);
        if (target == owner) {
            return Optional.empty();
        }
        if (target == rootVehicle(owner)) {
            return Optional.empty();
        }
        if (!evaluatedTargets.add(target.getUUID())) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    static Set<UUID> mountedStackIds(Entity entity) {
        Set<UUID> ids = new HashSet<>();
        Entity root = rootVehicle(entity);
        collectPassengerIds(root, ids);
        return ids;
    }

    static boolean isMountedCorrection(Entity originalCandidate, Entity correctionTarget) {
        return originalCandidate != correctionTarget || correctionTarget.isVehicle();
    }

    private static Entity rootVehicle(Entity entity) {
        Entity current = entity;
        while (current.getVehicle() != null) {
            current = current.getVehicle();
        }
        return current;
    }

    private static void collectPassengerIds(Entity entity, Set<UUID> ids) {
        ids.add(entity.getUUID());
        for (Entity passenger : entity.getPassengers()) {
            collectPassengerIds(passenger, ids);
        }
    }
}
