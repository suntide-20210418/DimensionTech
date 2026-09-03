package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.Tier5MythicShellChikensVoidStructreResourceMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier5MythicShellChikensVoidStructreResourceMinerBlock extends BaseMinerBlock {
    public Tier5MythicShellChikensVoidStructreResourceMinerBlock(
            BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public int minerTier() {
        return 5;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier5MythicShellChikensVoidStructreResourceMinerBlockEntity(pos, state);
    }
}
