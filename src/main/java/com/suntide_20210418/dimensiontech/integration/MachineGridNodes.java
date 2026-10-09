package com.suntide_20210418.dimensiontech.integration;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/**
 * Builds a chamber's optional grid handle.
 *
 * <p>The AE2 implementation is named only inside the {@code isLoaded} guard, which is the same
 * isolation the rest of the AE2 bridge uses: without AE2 the class is never resolved, so no AE2
 * type is touched and the mod loads clean.
 */
public final class MachineGridNodes {
    private MachineGridNodes() {}

    /** The chamber's grid handle, or {@code null} when AE2 is not installed. */
    public static MachineGridNode create(BlockEntity owner) {
        return ModList.get().isLoaded("ae2")
                ? com.suntide_20210418.dimensiontech.integration.ae2.Ae2GridNode.create(owner)
                : null;
    }
}
