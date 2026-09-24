package com.suntide_20210418.dimensiontech.block;

import com.mojang.serialization.MapCodec;
import com.suntide_20210418.dimensiontech.block.entity.Tier4StructureMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier4StructureMinerBlock extends BaseMinerBlock {
    public static final MapCodec<Tier4StructureMinerBlock> CODEC =
            simpleCodec(Tier4StructureMinerBlock::new);

    public Tier4StructureMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int minerTier() {
        return 4;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier4StructureMinerBlockEntity(pos, state);
    }
}
