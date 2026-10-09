package com.suntide_20210418.dimensiontech.integration.ae2;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import com.suntide_20210418.dimensiontech.block.entity.ModBlockEntities;
import com.suntide_20210418.dimensiontech.integration.MachineGridNode;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers the item output chamber's grid node host capability.
 *
 * <p>This is the only place that declares an AE2 capability, so it must be reached only when AE2 is
 * loaded — {@code ModCapabilities} guards the call. The provider hands back the chamber's own grid
 * handle, which is the same object the chamber drives; a chamber with AE2 absent never reaches
 * here, and a chamber whose node has not been created yet simply answers {@code null}.
 */
public final class Ae2GridHosts {
    private Ae2GridHosts() {}

    public static void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.ITEM_OUTPUT_CHAMBER.get(),
                (chamber, side) -> {
                    MachineGridNode node = chamber.gridNode();
                    return node instanceof IInWorldGridNodeHost host ? host : null;
                });
    }
}
