package com.suntide_20210418.dimensiontech.integration.kubejs;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import net.minecraft.core.Direction;

public final class MinerBlockEntityJS {
    private final BaseMinerBlockEntity miner;

    public MinerBlockEntityJS(BaseMinerBlockEntity miner) {
        this.miner = miner;
    }

    public int energyStored() {
        return miner.getEnergyStored();
    }

    public int energyCapacity() {
        return miner.getBaseEnergyCapacity();
    }

    public int effectiveEnergyCapacity() {
        return miner.getEnergyStorage().getMaxEnergyStored();
    }

    public int energyConsumption() {
        return miner.getEffectiveEnergyConsumption();
    }

    public int workingThreadCount() {
        return miner.getWorkingThreadCount();
    }

    public double efficiency() {
        return miner.getEffectiveMachineEfficiency();
    }

    public double luck() {
        return miner.getEffectiveMachineLuck();
    }

    public int baseParallel() {
        return miner.getEffectiveBaseParallel();
    }

    public int progress() {
        return miner.getProgress();
    }

    public int processingTime() {
        return miner.getProcessingTime();
    }

    public int slotCount() {
        return miner.getSlotCountForScript();
    }

    public int slotProgress(int slot) {
        return miner.getSlotProgress(slot);
    }

    public boolean slotEnabled(int slot) {
        return miner.isSlotEnabled(slot);
    }

    public void setSlotEnabled(int slot, boolean enabled) {
        if (miner.isSlotEnabled(slot) != enabled) miner.toggleSlotEnabled(slot);
    }

    public int pendingOutputCount() {
        return miner.getPendingOutputCount();
    }

    public boolean structureComplete() {
        return miner.isStructureComplete();
    }

    public int fluidAmount() {
        return miner.getFluidTank().getFluidAmount();
    }

    public int fluidCapacity() {
        return miner.getFluidTank().getCapacity();
    }

    public String requiredFluid() {
        var fluid = miner.getRequiredFluid();
        return fluid == null
                ? null
                : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    public boolean autoExtractFluid() {
        return miner.isAutoExtractFluidEnabled();
    }

    public void setAutoExtractFluid(boolean enabled) {
        if (miner.isAutoExtractFluidEnabled() != enabled) miner.toggleAutoExtractFluid();
    }

    public boolean equipmentDismantling() {
        return miner.isEquipmentDismantlingEnabled();
    }

    public void setEquipmentDismantling(boolean enabled) {
        if (miner.isEquipmentDismantlingEnabled() != enabled) miner.toggleEquipmentDismantling();
    }

    public boolean expectedItemDisabled(Object id) {
        return miner.isExpectedItemDisabled(DimensionTechJS.parse(id));
    }

    public void setExpectedItemDisabled(Object id, boolean disabled) {
        var item = DimensionTechJS.parse(id);
        if (miner.isExpectedItemDisabled(item) != disabled) miner.toggleExpectedItem(item);
    }

    public java.util.List<net.minecraft.resources.ResourceLocation> markedStructures() {
        return miner.getMarkedStructures();
    }

    public String blockId() {
        return miner.getBlockState().getBlock().builtInRegistryHolder().key().location().toString();
    }

    public int minerTier() {
        return miner.getMinerTier();
    }

    public String dimension() {
        return miner.getLevel() == null
                ? "minecraft:overworld"
                : miner.getLevel().dimension().location().toString();
    }

    public net.minecraft.core.BlockPos position() {
        return miner.getBlockPos();
    }

    public String redstoneMode() {
        return miner.getRedstoneMode().name().toLowerCase(java.util.Locale.ROOT);
    }

    public void setRedstoneMode(String mode) {
        BaseMinerBlockEntity.RedstoneMode target;
        try {
            target =
                    BaseMinerBlockEntity.RedstoneMode.valueOf(
                            mode.toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unknown redstone mode: " + mode, e);
        }
        for (int i = 0; i < BaseMinerBlockEntity.RedstoneMode.values().length; i++) {
            if (miner.getRedstoneMode() == target) return;
            miner.cycleRedstoneMode();
        }
    }

    public String outputMode() {
        return miner.getOutputState().name().toLowerCase(java.util.Locale.ROOT);
    }

    public void setOutputMode(String mode) {
        BaseMinerBlockEntity.OutputState target;
        try {
            target =
                    BaseMinerBlockEntity.OutputState.valueOf(
                            mode.toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unknown output mode: " + mode, e);
        }
        if (target == BaseMinerBlockEntity.OutputState.NONE)
            throw new IllegalArgumentException("Output mode cannot be none");
        if (miner.getOutputState() != target) miner.cycleOutputState();
    }

    public void setOutputFace(Direction direction, boolean enabled) {
        if (miner.isOutputFaceEnabled(direction) != enabled) miner.toggleOutputFace(direction);
    }

    public void setOutputFace(String direction, boolean enabled) {
        try {
            setOutputFace(Direction.valueOf(direction.toUpperCase(java.util.Locale.ROOT)), enabled);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unknown direction: " + direction, e);
        }
    }
}
