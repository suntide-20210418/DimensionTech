package com.suntide_20210418.dimensiontech.integration.kubejs;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;

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
        return miner.getEffectiveEnergyCapacity();
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
        return miner.getFluidAmount();
    }

    public int fluidCapacity() {
        return miner.getFluidCapacity();
    }

    public String requiredFluid() {
        var fluid = miner.getRequiredFluid();
        return fluid == null
                ? null
                : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    /**
     * Auto-pull now lives on the fluid input chamber, so this reads — and sets — that chamber. It
     * reports false when the ring has no fluid chamber, which is also the reason a machine without
     * one can never start a cycle.
     */
    public boolean autoExtractFluid() {
        return miner.isFluidAutoPullEnabled();
    }

    public void setAutoExtractFluid(boolean enabled) {
        miner.setFluidAutoPull(enabled);
    }

    public boolean hasFluidChamber() {
        return miner.hasFluidChamber();
    }

    public boolean hasEnergyChamber() {
        return miner.hasEnergyChamber();
    }

    public boolean hasItemChamber() {
        return miner.hasItemChamber();
    }

    public boolean equipmentDismantling() {
        return miner.isEquipmentDismantlingEnabled();
    }

    public void setEquipmentDismantling(boolean enabled) {
        if (miner.isEquipmentDismantlingEnabled() != enabled) miner.toggleEquipmentDismantling();
    }

    public boolean expectedItemDisabled(int slot, Object id) {
        return miner.isExpectedItemDisabled(slot, DimensionTechJS.parse(id));
    }

    public void setExpectedItemDisabled(int slot, Object id, boolean disabled) {
        var item = DimensionTechJS.parse(id);
        if (miner.isExpectedItemDisabled(slot, item) != disabled)
            miner.toggleExpectedItem(slot, item);
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
}
