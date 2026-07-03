package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalTargetSelectorTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TARGET_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final Vec3 USER_POSITION = Vec3.ZERO;
    private static final CausalTrackingPolicy TRACKING_POLICY =
            new CausalTrackingPolicy(5.0D, 12.0D, 16.0D);
    private static final CausalTargetSelector SELECTOR =
            new CausalTargetSelector(CausalTargetSelectionRules.DEFAULT);

    @Test
    void unlockedCandidateRejectedOutsideSessionRadius() {
        CausalTargetSelection selection =
                select(Optional.empty(), candidate(TARGET_A, new Vec3(6.0D, 0.0D, 0.0D)));

        assertTrue(selection.selectedCandidate().isEmpty());
    }

    @Test
    void lockedTargetRemainsTrackedOutsideSessionRadius() {
        CausalLink previous = link(TARGET_A, 0);

        CausalTargetSelection selection =
                select(Optional.of(previous), candidate(TARGET_A, new Vec3(8.0D, 0.0D, 0.0D)));

        assertEquals(TARGET_A, selectedTarget(selection));
    }

    @Test
    void lockedTargetExpiresBeyondCausalTrackingRadius() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalLink link = link(TARGET_A, 0);
        manager.putLink(link);

        CausalTargetCandidate farTarget = candidate(TARGET_A, new Vec3(20.0D, 0.0D, 0.0D));

        CausalLink expired =
                manager.expireIfBeyondMaxDistance(
                                OWNER_ID, farTarget, TRACKING_POLICY, USER_POSITION, 10)
                        .orElseThrow();

        assertEquals(CausalLinkState.EXPIRED, expired.state());
    }

    @Test
    void selectsClosestTargetWhenIntentIsEqual() {
        CausalTargetSelection selection =
                select(
                        Optional.empty(),
                        Vec3.ZERO,
                        Vec3.ZERO,
                        candidate(TARGET_A, new Vec3(4.0D, 0.0D, 0.0D)),
                        candidate(TARGET_B, new Vec3(2.0D, 0.0D, 0.0D)));

        assertEquals(TARGET_B, selectedTarget(selection));
    }

    @Test
    void selectsLookedAtTargetOverSlightlyCloserTarget() {
        CausalTargetSelection selection =
                select(
                        Optional.empty(),
                        new Vec3(1.0D, 0.0D, 0.0D),
                        Vec3.ZERO,
                        candidate(TARGET_A, new Vec3(0.0D, 0.0D, 2.5D)),
                        candidate(TARGET_B, new Vec3(3.0D, 0.0D, 0.0D)));

        assertEquals(TARGET_B, selectedTarget(selection));
    }

    @Test
    void selectsMovementDirectionTarget() {
        CausalTargetSelection selection =
                select(
                        Optional.empty(),
                        Vec3.ZERO,
                        new Vec3(0.0D, 0.0D, 1.0D),
                        candidate(TARGET_A, new Vec3(2.0D, 0.0D, 0.0D)),
                        candidate(TARGET_B, new Vec3(0.0D, 0.0D, 3.0D)));

        assertEquals(TARGET_B, selectedTarget(selection));
    }

    @Test
    void keepsPreviousTargetWhenScoresAreClose() {
        CausalLink previous = link(TARGET_A, 0);

        CausalTargetSelection selection =
                select(
                        Optional.of(previous),
                        Vec3.ZERO,
                        Vec3.ZERO,
                        candidate(TARGET_A, new Vec3(3.8D, 0.0D, 0.0D)),
                        candidate(TARGET_B, new Vec3(3.6D, 0.0D, 0.0D)));

        assertEquals(TARGET_A, selectedTarget(selection));
    }

    @Test
    void switchesTargetWhenNewScoreSignificantlyHigher() {
        CausalLink previous = link(TARGET_A, 0);

        CausalTargetSelection selection =
                select(
                        Optional.of(previous),
                        new Vec3(0.0D, 0.0D, 1.0D),
                        Vec3.ZERO,
                        candidate(TARGET_A, new Vec3(4.5D, 0.0D, 0.0D)),
                        candidate(TARGET_B, new Vec3(0.0D, 0.0D, 1.5D)));

        assertEquals(TARGET_B, selectedTarget(selection));
    }

    @Test
    void rejectsSelfTarget() {
        CausalTargetSelection selection =
                select(
                        Optional.empty(),
                        new CausalTargetCandidate(
                                OWNER_ID,
                                new Vec3(1.0D, 0.0D, 0.0D),
                                boundsAt(Vec3.ZERO),
                                true,
                                true));

        assertTrue(selection.selectedCandidate().isEmpty());
    }

    @Test
    void rejectsTargetWithoutTemporalAdvantage() {
        CausalTargetSelection selection =
                select(
                        Optional.empty(),
                        new CausalTargetCandidate(
                                TARGET_A,
                                new Vec3(1.0D, 0.0D, 0.0D),
                                boundsAt(Vec3.ZERO),
                                false,
                                false));

        assertTrue(selection.selectedCandidate().isEmpty());
    }

    private static CausalTargetSelection select(
            Optional<CausalLink> previousLink, CausalTargetCandidate... candidates) {
        return select(previousLink, new Vec3(1.0D, 0.0D, 0.0D), Vec3.ZERO, candidates);
    }

    private static CausalTargetSelection select(
            Optional<CausalLink> previousLink,
            Vec3 lookDirection,
            Vec3 movementDirection,
            CausalTargetCandidate... candidates) {
        return SELECTOR.select(
                OWNER_ID,
                USER_POSITION,
                lookDirection,
                movementDirection,
                TRACKING_POLICY,
                previousLink,
                List.of(candidates),
                10);
    }

    private static UUID selectedTarget(CausalTargetSelection selection) {
        return selection.selectedCandidate().orElseThrow().targetId();
    }

    private static CausalTargetCandidate candidate(UUID targetId, Vec3 position) {
        return CausalTargetCandidate.target(targetId, position, boundsAt(position));
    }

    private static AABB boundsAt(Vec3 position) {
        return new AABB(
                position.x - 0.5D,
                position.y,
                position.z - 0.5D,
                position.x + 0.5D,
                position.y + 1.8D,
                position.z + 0.5D);
    }

    private static CausalLink link(UUID targetId, int selectedTick) {
        return CausalLink.softLocked(OWNER_ID, targetId, selectedTick, frame(targetId));
    }

    private static CausalPhantomFrame frame(UUID targetId) {
        return new CausalPhantomFrame(
                targetId,
                0,
                Vec3.ZERO,
                boundsAt(Vec3.ZERO),
                CausalPhantomPoseState.standing(),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }
}
