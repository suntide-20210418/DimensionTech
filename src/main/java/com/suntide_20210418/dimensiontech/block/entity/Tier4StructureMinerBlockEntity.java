package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier4StructureMinerBlockEntity extends BaseMinerBlockEntity {
    public Tier4StructureMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIER_4_STRUCTURE_MINER.get(), pos, state);
    }

    private ModConfigs.StructureMinerTierConfig config() {
        return ModConfigs.TIERS[3];
    }

    @Override
    protected int getSlotCount() {
        return config().slotCount();
    }

    @Override
    protected String getTranslationName() {
        return "tier_4_structure_miner";
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
