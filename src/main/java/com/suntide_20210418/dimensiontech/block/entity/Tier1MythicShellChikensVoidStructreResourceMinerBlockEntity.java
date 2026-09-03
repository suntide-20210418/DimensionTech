package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier1MythicShellChikensVoidStructreResourceMinerBlockEntity
        extends BaseMinerBlockEntity {

    public Tier1MythicShellChikensVoidStructreResourceMinerBlockEntity(
            BlockPos position, BlockState blockState) {
        super(ModBlockEntities.TIER_1_MYTHIC_MINER.get(), position, blockState);
    }

    @Override
    protected int getSlotCount() {
        return ModConfigs.TIERS[0].slotCount();
    }

    @Override
    protected String getTranslationName() {
        return "tier_1_mythic_miner";
    }

    @Override
    protected int getBaseParallel() {
        return ModConfigs.TIERS[0].baseParallel();
    }

    @Override
    protected float getMachineLuck() {
        return ModConfigs.TIERS[0].baseLuck();
    }

    @Override
    protected int getEnergyCapacity() {
        return ModConfigs.TIERS[0].energyCapacity();
    }

    @Override
    public int getEnergyConsumption() {
        return ModConfigs.TIERS[0].energyConsumption();
    }

    @Override
    protected double getMachineEfficiency() {
        return ModConfigs.TIERS[0].efficiency();
    }

    @Override
    protected double getQuantityReference() {
        return ModConfigs.TIERS[0].quantityReference();
    }
}
