package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MinerUpgradeControllerTest {
    @Test
    void upgradeStateDoesNotExposeMutableCountArray() {
        int[] counts = {1, 2, 3};
        var state = new MinerUpgradeController.UpgradeState(
                1.0D, 1.0D, 100, 0.0D, 1.0D, 0, 0, 0, 0, 0, counts);
        counts[0] = 99;
        int[] exposed = state.upgradeCountsByTypeAndTier();
        exposed[1] = 88;
        assertEquals(1, state.countFor(MythicMinerUpgradeBlock.Type.EFFICIENCY, 1));
        assertEquals(2, state.countFor(MythicMinerUpgradeBlock.Type.EFFICIENCY, 2));
    }
}
