package com.time_engine.engine.common.causal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CausalLinkManagerTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER_TARGET_ID =
            UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void softRefreshDoesNotDowngradeActiveHardLock() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalTargetSelection selection = CausalTargetSelection.selected(candidate(), 10.0D);
        manager.updateSoftLock(OWNER_ID, selection, 0, frame(), 0.0D);
        manager.hardLock(OWNER_ID, 1, 20);

        CausalLink refreshed =
                manager.updateSoftLock(OWNER_ID, selection, 2, frame(), 0.5D).orElseThrow();

        assertEquals(CausalLinkState.HARD_LOCK, refreshed.state());
    }

    @Test
    void hardLockPreservesPursuitProgress() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalTargetSelection selection = CausalTargetSelection.selected(candidate(), 10.0D);
        advanceProgress(manager, selection, 0.25D);

        CausalLink hardLocked = manager.hardLock(OWNER_ID, 1, 20).orElseThrow();

        assertEquals(CausalLinkState.HARD_LOCK, hardLocked.state());
        assertEquals(0.25D, hardLocked.progress(), 1.0E-8D);
    }

    @Test
    void sameTargetSoftRefreshCanMovePursuitProgressBackwards() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalTargetSelection selection = CausalTargetSelection.selected(candidate(), 10.0D);
        advanceProgress(manager, selection, 0.5D);

        CausalLink refreshed =
                manager.updateSoftLock(OWNER_ID, selection, 2, frame(), frame(OWNER_ID), 0.25D)
                        .orElseThrow();

        assertEquals(CausalLinkState.SOFT_LOCK, refreshed.state());
        assertEquals(0.25D, refreshed.progress(), 1.0E-8D);
    }

    @Test
    void sameTargetRefreshCanMovePursuitProgressForward() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalTargetSelection selection = CausalTargetSelection.selected(candidate(), 10.0D);
        advanceProgress(manager, selection, 0.25D);

        CausalLink refreshed =
                manager.updateSoftLock(OWNER_ID, selection, 2, frame(), frame(OWNER_ID), 0.5D)
                        .orElseThrow();

        assertEquals(0.5D, refreshed.progress(), 1.0E-8D);
    }

    @Test
    void softRefreshCannotSwitchActiveHardLockToDifferentTarget() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.updateSoftLock(
                OWNER_ID,
                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                0,
                frame(TARGET_ID),
                0.0D);
        manager.hardLock(OWNER_ID, 1, 20);

        CausalLink refreshed =
                manager.updateSoftLock(
                                OWNER_ID,
                                CausalTargetSelection.selected(candidate(OTHER_TARGET_ID), 20.0D),
                                2,
                                frame(OTHER_TARGET_ID),
                                0.5D)
                        .orElseThrow();

        assertEquals(TARGET_ID, refreshed.targetId());
        assertEquals(CausalLinkState.HARD_LOCK, refreshed.state());
    }

    @Test
    void viewsExposeReadOnlyDiagnosticsForManagedLinks() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.updateSoftLock(
                OWNER_ID,
                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                5,
                frame(TARGET_ID),
                0.25D);

        Collection<CausalLinkView> views = manager.views(8);

        CausalLinkView view = views.iterator().next();
        assertEquals(OWNER_ID, view.ownerId());
        assertEquals(TARGET_ID, view.targetId());
        assertEquals(3, view.ageTicks());
        assertEquals(3, view.ticksSinceUpdate());
    }

    @Test
    void softLockStoresSeparateTargetAndOwnerFrames() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalPhantomFrame targetFrame = frame(TARGET_ID, new Vec3(10.0D, 0.0D, 0.0D));
        CausalPhantomFrame ownerFrame = frame(OWNER_ID, new Vec3(1.0D, 0.0D, 0.0D));

        CausalLink created =
                manager.updateSoftLock(
                                OWNER_ID,
                                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                                10,
                                targetFrame,
                                ownerFrame,
                                0.25D)
                        .orElseThrow();

        assertEquals(targetFrame, created.latestFrame());
        assertEquals(targetFrame, created.originTargetFrame());
        assertEquals(ownerFrame, created.originOwnerFrame());
        assertEquals(ownerFrame, created.ownerFrame());

        CausalPhantomFrame refreshedTargetFrame = frame(TARGET_ID, new Vec3(11.0D, 0.0D, 0.0D));
        CausalPhantomFrame refreshedOwnerFrame = frame(OWNER_ID, new Vec3(2.0D, 0.0D, 0.0D));
        CausalLink refreshed =
                manager.updateSoftLock(
                                OWNER_ID,
                                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                                11,
                                refreshedTargetFrame,
                                refreshedOwnerFrame,
                                0.25D)
                        .orElseThrow();

        assertEquals(refreshedTargetFrame, refreshed.latestFrame());
        assertEquals(targetFrame, refreshed.originTargetFrame());
        assertEquals(ownerFrame, refreshed.originOwnerFrame());
        assertEquals(refreshedOwnerFrame, refreshed.ownerFrame());
        assertEquals(0.25D, refreshed.progress(), 1.0E-8D);
    }

    @Test
    void breakLinkMarksActiveLinkAsBroken() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.updateSoftLock(
                OWNER_ID,
                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                0,
                frame(TARGET_ID),
                0.0D);

        CausalLink broken = manager.breakLink(OWNER_ID, 5).orElseThrow();

        assertEquals(CausalLinkState.BROKEN, broken.state());
        assertEquals(5, broken.lastUpdatedTick());
    }

    @Test
    void forceDebugStateFreezesManagedLink() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.updateSoftLock(
                OWNER_ID,
                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                0,
                frame(TARGET_ID),
                0.0D);

        CausalLink forced =
                manager.forceDebugState(OWNER_ID, CausalLinkState.BROKEN, 5, 20).orElseThrow();

        assertEquals(CausalLinkState.BROKEN, forced.state());
        assertEquals(5, forced.lastUpdatedTick());
        assertTrue(manager.isDebugFrozen(OWNER_ID));
    }

    @Test
    void forceDebugLinkCreatesFrozenLinkWhenNoneExists() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalPhantomFrame targetFrame = frame(TARGET_ID);
        CausalPhantomFrame ownerFrame = frame(OWNER_ID);

        CausalLink forced =
                manager.forceDebugLink(
                        OWNER_ID,
                        TARGET_ID,
                        CausalLinkState.HARD_LOCK,
                        5,
                        20,
                        targetFrame,
                        ownerFrame);

        assertEquals(OWNER_ID, forced.ownerId());
        assertEquals(TARGET_ID, forced.targetId());
        assertEquals(CausalLinkState.HARD_LOCK, forced.state());
        assertTrue(manager.isDebugFrozen(OWNER_ID));
    }

    @Test
    void clearDebugStateResumesRuntimeOwnership() {
        CausalLinkManager manager = new CausalLinkManager();
        manager.updateSoftLock(
                OWNER_ID,
                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                0,
                frame(TARGET_ID),
                0.0D);
        manager.forceDebugState(OWNER_ID, CausalLinkState.EXPIRED, 5, 20);

        manager.clearDebugState(OWNER_ID);

        assertFalse(manager.isDebugFrozen(OWNER_ID));
    }

    @Test
    void targetSwitchResetsOriginFrames() {
        CausalLinkManager manager = new CausalLinkManager();
        CausalPhantomFrame firstTargetFrame = frame(TARGET_ID, new Vec3(10.0D, 0.0D, 0.0D));
        CausalPhantomFrame firstOwnerFrame = frame(OWNER_ID, new Vec3(1.0D, 0.0D, 0.0D));
        manager.updateSoftLock(
                OWNER_ID,
                CausalTargetSelection.selected(candidate(TARGET_ID), 10.0D),
                10,
                firstTargetFrame,
                firstOwnerFrame,
                0.0D);

        CausalPhantomFrame nextTargetFrame = frame(OTHER_TARGET_ID, new Vec3(20.0D, 0.0D, 0.0D));
        CausalPhantomFrame nextOwnerFrame = frame(OWNER_ID, new Vec3(2.0D, 0.0D, 0.0D));
        CausalLink switched =
                manager.updateSoftLock(
                                OWNER_ID,
                                CausalTargetSelection.selected(candidate(OTHER_TARGET_ID), 20.0D),
                                20,
                                nextTargetFrame,
                                nextOwnerFrame,
                                0.0D)
                        .orElseThrow();

        assertEquals(OTHER_TARGET_ID, switched.targetId());
        assertEquals(nextTargetFrame, switched.originTargetFrame());
        assertEquals(nextOwnerFrame, switched.originOwnerFrame());
    }

    private static CausalTargetCandidate candidate() {
        return candidate(TARGET_ID);
    }

    private static CausalTargetCandidate candidate(UUID targetId) {
        return CausalTargetCandidate.target(targetId, Vec3.ZERO, bounds());
    }

    private static void advanceProgress(
            CausalLinkManager manager, CausalTargetSelection selection, double progress) {
        manager.updateSoftLock(OWNER_ID, selection, 0, frame(), 0.0D);
        manager.updateSoftLock(OWNER_ID, selection, 1, frame(), progress);
    }

    private static CausalPhantomFrame frame() {
        return frame(TARGET_ID);
    }

    private static CausalPhantomFrame frame(UUID targetId) {
        return frame(targetId, Vec3.ZERO);
    }

    private static CausalPhantomFrame frame(UUID targetId, Vec3 anchor) {
        return new CausalPhantomFrame(
                targetId,
                0,
                anchor,
                bounds(),
                CausalPhantomPoseState.standing(),
                CausalPhantomActionState.NONE,
                CausalPhantomEquipmentState.EMPTY);
    }

    private static AABB bounds() {
        return new AABB(-0.5D, 0.0D, -0.5D, 0.5D, 1.8D, 0.5D);
    }
}
