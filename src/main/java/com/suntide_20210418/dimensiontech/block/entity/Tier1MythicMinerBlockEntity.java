package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier1MythicMinerBlockEntity extends BaseMinerBlockEntity {

    public Tier1MythicMinerBlockEntity(BlockPos position, BlockState blockState) {
        super(ModBlockEntities.TIER_1_MYTHIC_MINER.get(), position, blockState);
    }

    @Override
    protected int getSlotCount() {
        return ModConfigs.TIER_1_MYTHIC_MINER.slotCount();
    }

    @Override
    protected String getTranslationName() {
        return "tier_1_mythic_miner";
    }

    @Override
    protected int getBaseParallel() {
        return ModConfigs.TIER_1_MYTHIC_MINER.baseParallel();
    }

    @Override
    protected float getMachineLuck() {
        return ModConfigs.TIER_1_MYTHIC_MINER.baseLuck();
    }

    @Override
    protected int getEnergyCapacity() {
        return ModConfigs.TIER_1_MYTHIC_MINER.energyCapacity();
    }

    @Override
    public int getEnergyConsumption() {
        return ModConfigs.TIER_1_MYTHIC_MINER.energyConsumption();
    }

    @Override
    protected double getMachineEfficiency() {
        return ModConfigs.TIER_1_MYTHIC_MINER.efficiency();
    }

    @Override
    protected double getQuantityReference() {
        return ModConfigs.TIER_1_MYTHIC_MINER.quantityReference();
    }
}
