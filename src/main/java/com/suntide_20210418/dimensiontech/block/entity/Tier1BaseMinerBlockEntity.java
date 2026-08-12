package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier1BaseMinerBlockEntity extends BaseMinerBlockEntity {

    public Tier1BaseMinerBlockEntity(BlockPos position, BlockState blockState) {
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
    protected float getDrawLuck() {
        return ModConfigs.TIER_1_MYTHIC_MINER.baseLuck();
    }

    @Override
    protected int getDrawParallel() {
        return ModConfigs.TIER_1_MYTHIC_MINER.baseParallel();
    }

    @Override
    protected int getEnergyCapacity() {
        return ModConfigs.TIER_1_MYTHIC_MINER.energyCapacity();
    }

    @Override
    protected int getEnergyConsumption() {
        return ModConfigs.TIER_1_MYTHIC_MINER.energyConsumption();
    }

    @Override
    protected int getProcessingTime() {
        return ModConfigs.TIER_1_MYTHIC_MINER.processingTime();
    }
}
