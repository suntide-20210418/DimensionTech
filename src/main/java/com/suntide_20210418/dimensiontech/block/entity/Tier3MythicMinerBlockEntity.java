package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier3MythicMinerBlockEntity extends BaseMinerBlockEntity {
    public Tier3MythicMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIER_3_MYTHIC_MINER.get(), pos, state);
    }

    private ModConfigs.MythicMinerTierConfig config() {
        return ModConfigs.TIERS[2];
    }

    @Override
    protected int getSlotCount() {
        return config().slotCount();
    }

    @Override
    protected String getTranslationName() {
        return "tier_3_mythic_miner";
    }

    @Override
    protected int getBaseParallel() {
        return config().baseParallel();
    }

    @Override
    protected float getMachineLuck() {
        return config().baseLuck();
    }

    @Override
    protected int getEnergyCapacity() {
        return config().energyCapacity();
    }

    @Override
    public int getEnergyConsumption() {
        return config().energyConsumption();
    }

    @Override
    protected double getMachineEfficiency() {
        return config().efficiency();
    }

    @Override
    protected double getQuantityReference() {
        return config().quantityReference();
    }
}
