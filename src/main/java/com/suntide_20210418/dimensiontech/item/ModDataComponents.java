package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item data components owned by the mod.
 *
 * <p>1.21 removed item stack NBT entirely, so anything that 1.20.1 stored under a stack's tag now
 * needs a component. The structure marker payload is the mod's case of that.
 */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DimensionTechMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StructureMarkerData>>
            STRUCTURE_MARKER =
                    DATA_COMPONENT_TYPES.register(
                            "structure_marker_data",
                            () ->
                                    DataComponentType.<StructureMarkerData>builder()
                                            // Persistent so the payload survives save/load, and
                                            // network-synchronized because a refreshed marker and
                                            // an
                                            // operator analysis result both travel inside packets.
                                            .persistent(StructureMarkerData.CODEC)
                                            .networkSynchronized(StructureMarkerData.STREAM_CODEC)
                                            .build());

    private ModDataComponents() {}

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
