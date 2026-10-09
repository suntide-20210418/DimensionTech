package com.suntide_20210418.dimensiontech.block;

import com.mojang.serialization.MapCodec;
import com.suntide_20210418.dimensiontech.block.entity.ItemOutputChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The item output chamber: the miner's output routing, moved out of the controller and into a
 * casing block.
 *
 * <p>The chamber ejects one stack per attempt into an adjacent container, so a large batch drains
 * group by group instead of all at once. When AE2 is installed it is also an ME device: it joins
 * the network from its own position and, while online, pushes the whole queue into network storage
 * in a single attempt instead.
 */
public final class ItemOutputChamberBlock extends MinerChamberBlock {
    public static final MapCodec<ItemOutputChamberBlock> CODEC =
            simpleCodec(ItemOutputChamberBlock::new);

    public ItemOutputChamberBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(BlockPos position, BlockState blockState) {
        return new ItemOutputChamberBlockEntity(position, blockState);
    }
}
