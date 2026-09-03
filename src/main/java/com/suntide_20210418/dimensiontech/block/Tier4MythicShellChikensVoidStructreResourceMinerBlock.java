package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.Tier4MythicShellChikensVoidStructreResourceMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier4MythicShellChikensVoidStructreResourceMinerBlock extends BaseMinerBlock {
    public Tier4MythicShellChikensVoidStructreResourceMinerBlock(
            BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int minerTier() {
        return 4;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier4MythicShellChikensVoidStructreResourceMinerBlockEntity(pos, state);
    }
}
