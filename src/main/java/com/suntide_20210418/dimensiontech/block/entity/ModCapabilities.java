package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.integration.ae2.Ae2GridHosts;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Central NeoForge capability registration. Every provider this mod exposes is declared here; the
 * block entities themselves only keep the handler fields.
 *
 * <p>Since the miner's fluid and energy moved into the chambers, the miner itself only exposes its
 * marker slots. Fluid, energy and the AE grid host are the chambers' business.
 */
public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        // Every tier is its own BlockEntityType, so the miner capabilities are registered once per
        // type instead of once for the shared base class.
        registerMiner(event, ModBlockEntities.TIER_1_STRUCTURE_MINER.get());
        registerMiner(event, ModBlockEntities.TIER_2_STRUCTURE_MINER.get());
        registerMiner(event, ModBlockEntities.TIER_3_STRUCTURE_MINER.get());
        registerMiner(event, ModBlockEntities.TIER_4_STRUCTURE_MINER.get());
        registerMiner(event, ModBlockEntities.TIER_5_STRUCTURE_MINER.get());
        registerMiner(event, ModBlockEntities.TIER_6_STRUCTURE_MINER.get());

        // Fluid only ever enters the machine through the fluid chamber, and the chamber hands the
        // tank out as fill-only: it is an input, and nothing may pump the working fluid back out.
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_INPUT_CHAMBER.get(),
                (chamber, side) -> chamber.inputHandler());
        // The energy chamber is the machine's one FE buffer.
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_INPUT_CHAMBER.get(),
                (chamber, side) -> chamber.energy());

        // The AE2 capability type only exists when AE2 does. The bridge class is named inside the
        // guard, so a client without AE2 never resolves it.
        if (ModList.get().isLoaded("ae2")) {
            Ae2GridHosts.registerCapability(event);
        }

        BlockEntityType<StructureReactorBlockEntity> reactor =
                ModBlockEntities.STRUCTURE_REACTOR.get();
        // Insert-only: both slots are material the reactor consumes, so pipes may feed them but
        // never empty them. The menu still holds the raw handler for manual removal.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                reactor,
                (blockEntity, side) -> blockEntity.insertOnlyItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                reactor,
                (blockEntity, side) -> reactorFluidHandler(blockEntity, side));
    }

    private static <BE extends BaseMinerBlockEntity> void registerMiner(
            RegisterCapabilitiesEvent event, BlockEntityType<BE> type) {
        // Insert-only: every marker slot is material the miner consumes, so pipes and buses may
        // load them but never empty them. The menu keeps the raw handler for manual removal.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (miner, side) -> miner.insertOnlyItemHandler());
    }

    /**
     * Without a face context the reactor exposes both tanks as one handler; with a face it exposes
     * that face's view of them, and a disabled face exposes nothing.
     */
    private static net.neoforged.neoforge.fluids.capability.IFluidHandler reactorFluidHandler(
            StructureReactorBlockEntity reactor, net.minecraft.core.Direction side) {
        if (side == null) return reactor.dualTankHandler();
        if (reactor.getFluidFaceMode(side) == StructureReactorBlockEntity.FluidFaceMode.DISABLED) {
            return null;
        }
        return reactor.faceFluidHandler(side);
    }
}
