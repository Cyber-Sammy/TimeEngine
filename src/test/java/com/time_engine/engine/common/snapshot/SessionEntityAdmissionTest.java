package com.time_engine.engine.common.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionEntityAdmissionTest {
    private static final UUID FIRST = UUID.fromString("571424a6-df09-439e-aec2-ad505e14f242");
    private static final UUID SECOND = UUID.fromString("bc214138-fd45-4718-817b-6e9814ebc5e4");
    private static final UUID THIRD = UUID.fromString("9234371b-ebd5-4e71-9b24-861fa50f58c9");

    @Test
    void disabledDynamicTrackingFreezesAfterFirstScanEvenWhenEmpty() {
        SessionEntityAdmission admission = new SessionEntityAdmission();

        admission.update(List.of(), Set.of(), false, 10, 100);
        admission.update(List.of(FIRST), Set.of(FIRST), false, 10, 101);

        assertFalse(admission.contains(FIRST));
        assertEquals(Set.of(), admission.admittedIds());
    }

    @Test
    void dynamicTrackingEvictsEntitiesThatAreNoLongerRetainable() {
        SessionEntityAdmission admission = new SessionEntityAdmission();

        admission.update(List.of(FIRST, SECOND), Set.of(FIRST, SECOND), true, 2, 100);
        SessionEntityAdmission.AdmissionUpdate update =
                admission.update(List.of(THIRD), Set.of(SECOND, THIRD), true, 2, 101);

        assertFalse(admission.contains(FIRST));
        assertTrue(admission.contains(SECOND));
        assertTrue(admission.contains(THIRD));
        assertEquals(1, update.evicted());
        assertEquals(1, update.newlyAdmitted());
    }

    @Test
    void retainedBoundaryEntityKeepsSlotUntilItLeavesRetainableArea() {
        SessionEntityAdmission admission = new SessionEntityAdmission();

        admission.update(List.of(FIRST), Set.of(FIRST), true, 1, 100);
        SessionEntityAdmission.AdmissionUpdate update =
                admission.update(List.of(SECOND), Set.of(FIRST, SECOND), true, 1, 101);

        assertTrue(admission.contains(FIRST));
        assertFalse(admission.contains(SECOND));
        assertEquals(0, update.evicted());
        assertTrue(update.capReached());
    }

    @Test
    void admissionTickBelongsToFirstAdmission() {
        SessionEntityAdmission admission = new SessionEntityAdmission();

        admission.update(List.of(FIRST), Set.of(FIRST), true, 10, 100);
        admission.update(List.of(FIRST), Set.of(FIRST), true, 10, 120);

        assertEquals(100, admission.admissionTick(FIRST).orElseThrow());
    }
}
