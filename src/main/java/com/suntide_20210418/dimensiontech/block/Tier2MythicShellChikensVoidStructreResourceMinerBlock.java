package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.Tier2MythicShellChikensVoidStructreResourceMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier2MythicShellChikensVoidStructreResourceMinerBlock extends BaseMinerBlock {
    public Tier2MythicShellChikensVoidStructreResourceMinerBlock(
            BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int minerTier() {
        return 2;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier2MythicShellChikensVoidStructreResourceMinerBlockEntity(pos, state);
    }
}
