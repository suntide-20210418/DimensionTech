package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.Tier6MythicMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier6MythicMinerBlock extends BaseMinerBlock {
    public Tier6MythicMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int minerTier() {
        return 6;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier6MythicMinerBlockEntity(pos, state);
    }
}
