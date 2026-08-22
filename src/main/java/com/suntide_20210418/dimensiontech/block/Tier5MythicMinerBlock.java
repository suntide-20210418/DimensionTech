package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.Tier5MythicMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier5MythicMinerBlock extends BaseMinerBlock {
    public Tier5MythicMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int minerTier() {
        return 5;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier5MythicMinerBlockEntity(pos, state);
    }
}
