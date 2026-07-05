package com.time_engine.engine.common.causal;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class CausalTargetSelector {
    private final CausalTargetSelectionRules rules;

    public CausalTargetSelector(CausalTargetSelectionRules rules) {
        this.rules = rules;
    }

    public CausalTargetSelection select(
            UUID ownerId,
            Vec3 userPosition,
            Vec3 lookDirection,
            Vec3 movementDirection,
            CausalTrackingPolicy trackingPolicy,
            Optional<CausalLink> previousLink,
            Collection<CausalTargetCandidate> candidates,
            int serverTick) {
        CandidateScore bestScore =
                findBestScore(
                        ownerId,
                        userPosition,
                        lookDirection,
                        movementDirection,
                        trackingPolicy,
                        previousLink,
                        candidates,
                        serverTick);
        if (bestScore.isEmpty()) {
            return CausalTargetSelection.empty();
        }

        Optional<CausalLink> activePrevious = activePreviousLink(previousLink);
        if (shouldKeepPrevious(activePrevious, bestScore, serverTick)) {
            return selectionForPrevious(activePrevious.get(), bestScore);
        }

        return CausalTargetSelection.selected(bestScore.candidate(), bestScore.score());
    }

    private CandidateScore findBestScore(
            UUID ownerId,
            Vec3 userPosition,
            Vec3 lookDirection,
            Vec3 movementDirection,
            CausalTrackingPolicy trackingPolicy,
            Optional<CausalLink> previousLink,
            Collection<CausalTargetCandidate> candidates,
            int serverTick) {
        CandidateScore bestScore = CandidateScore.none();
        for (CausalTargetCandidate candidate : candidates) {
            if (!isSelectable(ownerId, userPosition, trackingPolicy, previousLink, candidate)) {
                continue;
            }

            double score =
                    scoreCandidate(
                            userPosition,
                            normalize(lookDirection),
                            normalize(movementDirection),
                            previousLink,
                            candidate,
                            serverTick);
            bestScore =
                    bestScore.chooseBetter(
                            candidate, score, isPreviousTarget(previousLink, candidate));
        }
        return bestScore;
    }

    private boolean isSelectable(
            UUID ownerId,
            Vec3 userPosition,
            CausalTrackingPolicy trackingPolicy,
            Optional<CausalLink> previousLink,
            CausalTargetCandidate candidate) {
        if (candidate.selfTarget()) {
            return false;
        }
        if (candidate.targetId().equals(ownerId)) {
            return false;
        }
        if (!candidate.temporalAdvantageAllowed()) {
            return false;
        }
        if (isPreviousTarget(previousLink, candidate)) {
            return trackingPolicy.keepsLockedTarget(userPosition, candidate);
        }
        return trackingPolicy.allowsUnlockedCandidate(userPosition, candidate);
    }

    private double scoreCandidate(
            Vec3 userPosition,
            Vec3 lookDirection,
            Vec3 movementDirection,
            Optional<CausalLink> previousLink,
            CausalTargetCandidate candidate,
            int serverTick) {
        Vec3 directionToGhost = candidate.directionFrom(userPosition);
        double distanceScore =
                rules.distanceWeight() / Math.max(1.0D, candidate.distanceTo(userPosition));
        double lookScore = directionalScore(lookDirection, directionToGhost, rules.lookWeight());
        double movementScore =
                directionalScore(movementDirection, directionToGhost, rules.movementWeight());
        double intentScore = intentConeBonus(lookDirection, directionToGhost);
        double previousBonus = previousBonus(previousLink, candidate);
        double hardLockBonus = hardLockBonus(previousLink, candidate, serverTick);
        return distanceScore
                + lookScore
                + movementScore
                + intentScore
                + previousBonus
                + hardLockBonus;
    }

    private boolean shouldKeepPrevious(
            Optional<CausalLink> previousLink, CandidateScore bestScore, int serverTick) {
        if (previousLink.isEmpty()) {
            return false;
        }
        CausalLink link = previousLink.get();
        if (!link.active()) {
            return false;
        }
        if (link.hardLockedAt(serverTick)) {
            return true;
        }
        if (serverTick - link.selectedTick() < rules.minimumLockTicks()) {
            return true;
        }
        if (bestScore.candidate().targetId().equals(link.targetId())) {
            return false;
        }
        return bestScore.score()
                < bestScore.previousTargetScore() + rules.targetSwitchScoreThreshold();
    }

    private CausalTargetSelection selectionForPrevious(
            CausalLink previousLink, CandidateScore bestScore) {
        return bestScore
                .previousCandidateOptional()
                .map(
                        candidate ->
                                CausalTargetSelection.selected(
                                        candidate, bestScore.previousTargetScore()))
                .orElseGet(
                        () ->
                                CausalTargetSelection.selected(
                                        bestScore.candidate(), bestScore.score()));
    }

    private Optional<CausalLink> activePreviousLink(Optional<CausalLink> previousLink) {
        return previousLink.filter(CausalLink::active);
    }

    private double directionalScore(Vec3 direction, Vec3 directionToGhost, double weight) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            return 0.0D;
        }
        return Math.max(0.0D, direction.dot(directionToGhost)) * weight;
    }

    private double intentConeBonus(Vec3 lookDirection, Vec3 directionToGhost) {
        if (lookDirection.lengthSqr() <= 1.0E-8D) {
            return 0.0D;
        }
        return lookDirection.dot(directionToGhost) >= rules.intentConeDot()
                ? rules.lookWeight()
                : 0.0D;
    }

    private double previousBonus(
            Optional<CausalLink> previousLink, CausalTargetCandidate candidate) {
        return isPreviousTarget(previousLink, candidate) ? rules.previousTargetBonus() : 0.0D;
    }

    private double hardLockBonus(
            Optional<CausalLink> previousLink, CausalTargetCandidate candidate, int serverTick) {
        if (!isPreviousTarget(previousLink, candidate)) {
            return 0.0D;
        }
        return previousLink.filter(link -> link.hardLockedAt(serverTick)).isPresent()
                ? rules.hardLockBonus()
                : 0.0D;
    }

    private static boolean isPreviousTarget(
            Optional<CausalLink> previousLink, CausalTargetCandidate candidate) {
        return previousLink
                .map(CausalLink::targetId)
                .filter(candidate.targetId()::equals)
                .isPresent();
    }

    private static Vec3 normalize(Vec3 vector) {
        if (vector.lengthSqr() <= 1.0E-8D) {
            return Vec3.ZERO;
        }
        return vector.normalize();
    }

    private record CandidateScore(
            CausalTargetCandidate candidate,
            double score,
            CausalTargetCandidate previousCandidate,
            double previousTargetScore) {
        static CandidateScore none() {
            return new CandidateScore(
                    null, Double.NEGATIVE_INFINITY, null, Double.NEGATIVE_INFINITY);
        }

        boolean isEmpty() {
            return candidate == null;
        }

        Optional<CausalTargetCandidate> previousCandidateOptional() {
            return Optional.ofNullable(previousCandidate);
        }

        CandidateScore chooseBetter(
                CausalTargetCandidate candidate, double candidateScore, boolean previousTarget) {
            CausalTargetCandidate nextPreviousCandidate = previousCandidate;
            double nextPreviousScore = previousTargetScore;
            if (previousTarget) {
                nextPreviousCandidate = candidate;
                nextPreviousScore = candidateScore;
            }
            if (candidateScore > score) {
                return new CandidateScore(
                        candidate, candidateScore, nextPreviousCandidate, nextPreviousScore);
            }
            return new CandidateScore(
                    this.candidate, score, nextPreviousCandidate, nextPreviousScore);
        }
    }
}
