package com.suntide_20210418.dimensiontech.block;

import com.mojang.serialization.MapCodec;
import com.suntide_20210418.dimensiontech.block.entity.Tier3StructureMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class Tier3StructureMinerBlock extends BaseMinerBlock {
    public static final MapCodec<Tier3StructureMinerBlock> CODEC =
            simpleCodec(Tier3StructureMinerBlock::new);

    public Tier3StructureMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int minerTier() {
        return 3;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tier3StructureMinerBlockEntity(pos, state);
    }
}
