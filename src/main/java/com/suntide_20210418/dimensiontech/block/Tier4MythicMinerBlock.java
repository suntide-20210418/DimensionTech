package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.Tier4MythicMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier4MythicMinerBlock extends BaseMinerBlock {
    public Tier4MythicMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int minerTier() {
        return 4;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier4MythicMinerBlockEntity(pos, state);
    }
}
