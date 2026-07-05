package com.time_engine.engine.common.causal;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class CausalLinkManager {
    private final Map<UUID, CausalLink> linksByOwner = new HashMap<>();

    public Optional<CausalLink> getLink(UUID ownerId) {
        return Optional.ofNullable(linksByOwner.get(ownerId));
    }

    public Collection<CausalLink> links() {
        return List.copyOf(linksByOwner.values());
    }

    public Collection<CausalLinkView> views(int serverTick) {
        return linksByOwner.values().stream()
                .map(link -> CausalLinkView.from(link, serverTick))
                .sorted(Comparator.comparing(CausalLinkView::ownerId))
                .toList();
    }

    public void putLink(CausalLink link) {
        linksByOwner.put(link.ownerId(), link);
    }

    public void clearLink(UUID ownerId) {
        linksByOwner.remove(ownerId);
    }

    public void clear() {
        linksByOwner.clear();
    }

    public Optional<CausalLink> updateSoftLock(
            UUID ownerId,
            CausalTargetSelection selection,
            int serverTick,
            CausalPhantomFrame frame,
            double progress) {
        if (selection.selectedCandidate().isEmpty()) {
            return Optional.empty();
        }

        UUID targetId = selection.selectedCandidate().orElseThrow().targetId();
        CausalLink updated =
                getLink(ownerId)
                        .filter(CausalLink::active)
                        .map(link -> link.refreshSoftLock(targetId, serverTick, frame, progress))
                        .orElseGet(
                                () -> CausalLink.softLocked(ownerId, targetId, serverTick, frame));
        putLink(updated);
        return Optional.of(updated);
    }

    public Optional<CausalLink> hardLock(UUID ownerId, int serverTick, int hardLockTicks) {
        Optional<CausalLink> link = getLink(ownerId).filter(CausalLink::active);
        link.map(activeLink -> activeLink.hardLock(serverTick, hardLockTicks))
                .ifPresent(this::putLink);
        return getLink(ownerId);
    }

    public Optional<CausalLink> expireIfBeyondMaxDistance(
            UUID ownerId,
            CausalTargetCandidate target,
            CausalTrackingPolicy trackingPolicy,
            Vec3 userPosition,
            int serverTick) {
        Optional<CausalLink> link = getLink(ownerId).filter(CausalLink::active);
        if (link.isEmpty()) {
            return Optional.empty();
        }
        if (!link.get().targetId().equals(target.targetId())) {
            return link;
        }
        if (!trackingPolicy.expiresLockedTarget(userPosition, target)) {
            return link;
        }

        CausalLink expired = link.get().expire(serverTick);
        putLink(expired);
        return Optional.of(expired);
    }
}
