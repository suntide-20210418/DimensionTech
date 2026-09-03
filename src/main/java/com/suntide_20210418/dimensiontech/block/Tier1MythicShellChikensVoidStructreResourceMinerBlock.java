package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.Tier1MythicShellChikensVoidStructreResourceMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class Tier1MythicShellChikensVoidStructreResourceMinerBlock extends BaseMinerBlock {

    public Tier1MythicShellChikensVoidStructreResourceMinerBlock(
            BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(BlockPos position, BlockState blockState) {
        return new Tier1MythicShellChikensVoidStructreResourceMinerBlockEntity(
                position, blockState);
    }
}
