package com.suntide_20210418.dimensiontech.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Central NeoForge capability registration. Every provider this mod exposes is declared here; the
 * block entities themselves only keep the handler fields.
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

        BlockEntityType<StructureReactorBlockEntity> reactor =
                ModBlockEntities.STRUCTURE_REACTOR.get();
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                reactor,
                (blockEntity, side) -> blockEntity.inventory());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                reactor,
                (blockEntity, side) -> reactorFluidHandler(blockEntity, side));
    }

    private static <BE extends BaseMinerBlockEntity> void registerMiner(
            RegisterCapabilitiesEvent event, BlockEntityType<BE> type) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, type, (miner, side) -> miner.getItemHandler());
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                type,
                (miner, side) -> miner.getEnergyStorage());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                type,
                (miner, side) -> minerFluidHandler(miner, side));
    }

    /**
     * The miner's single tank is exposed as a fill-only handler on input faces and a drain-only
     * handler on output faces. {@code side == null} carries no face context and means input, while a
     * disabled face (and a tier that needs no fluid at all) exposes nothing.
     */
    private static IFluidHandler minerFluidHandler(BaseMinerBlockEntity miner, Direction side) {
        if (!miner.requiresFluidInput()) return null;
        if (side == null) return miner.getFluidInputHandler();
        BaseMinerBlockEntity.FluidFaceMode mode = miner.getFluidFaceMode(side);
        if (mode == BaseMinerBlockEntity.FluidFaceMode.OUTPUT) return miner.getFluidOutputHandler();
        if (mode == BaseMinerBlockEntity.FluidFaceMode.INPUT) return miner.getFluidInputHandler();
        return null;
    }

    /**
     * Without a face context the reactor exposes both tanks as one handler; with a face it exposes
     * that face's view of them, and a disabled face exposes nothing.
     */
    private static IFluidHandler reactorFluidHandler(
            StructureReactorBlockEntity reactor, Direction side) {
        if (side == null) return reactor.dualTankHandler();
        if (reactor.getFluidFaceMode(side) == StructureReactorBlockEntity.FluidFaceMode.DISABLED) {
            return null;
        }
        return reactor.faceFluidHandler(side);
    }
}