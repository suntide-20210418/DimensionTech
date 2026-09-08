package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MinerScriptConfigTest {
    private static final ResourceLocation MINER =
            ResourceLocation.fromNamespaceAndPath("dimension_tech", "tier_1_mythic_miner");

    @Test
    void processingTimeRequiresTheNaturalObservationWindow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinerScriptConfig(MINER, 399, null, null, null, null, null, null));
        assertDoesNotThrow(
                () -> new MinerScriptConfig(MINER, 400, null, null, null, null, null, null));
        assertDoesNotThrow(
                () -> new MinerScriptConfig(MINER, 401, null, null, null, null, null, null));
    }
}
