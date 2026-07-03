package com.time_engine.sandevistan.activation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.time_engine.sandevistan.item.SandevistanTier;
import java.util.List;
import org.junit.jupiter.api.Test;

class SandevistanActivationServiceTest {
    @Test
    void chooseBestTierReturnsEmptyWhenNoTierExists() {
        assertTrue(SandevistanActivationService.chooseBestTier(List.of()).isEmpty());
    }

    @Test
    void chooseBestTierReturnsHighestAvailableTier() {
        assertEquals(
                SandevistanTier.MK3,
                SandevistanActivationService.chooseBestTier(
                                List.of(
                                        SandevistanTier.MK1,
                                        SandevistanTier.MK3,
                                        SandevistanTier.MK2))
                        .orElseThrow());
    }

    @Test
    void chooseBestTierFromGroupsReturnsHighestAvailableTierAcrossGroups() {
        assertEquals(
                SandevistanTier.MK3,
                SandevistanActivationService.chooseBestTierFromGroups(
                                List.of(
                                        List.of(SandevistanTier.MK1),
                                        List.of(SandevistanTier.MK2),
                                        List.of(SandevistanTier.MK3)))
                        .orElseThrow());
    }
}
