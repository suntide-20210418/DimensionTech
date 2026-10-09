package com.suntide_20210418.dimensiontech.block;

import com.mojang.serialization.MapCodec;
import com.suntide_20210418.dimensiontech.block.entity.EnergyInputChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * The energy input chamber: the miner's FE buffer, moved out of the controller and into a casing
 * block.
 *
 * <p>Its capacity mirrors the miner it serves, cached so the chamber can still take energy while
 * the multiblock is not standing (the controller only reports a meaningful capacity once its
 * upgrade bonuses are counted, which requires a complete structure). Right-click reports the stored
 * amount and the machine's energy consumption in chat.
 */
public final class EnergyInputChamberBlock extends MinerChamberBlock {
    public static final MapCodec<EnergyInputChamberBlock> CODEC =
            simpleCodec(EnergyInputChamberBlock::new);

    public EnergyInputChamberBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(BlockPos position, BlockState blockState) {
        return new EnergyInputChamberBlockEntity(position, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState blockState,
            Level level,
            BlockPos position,
            Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide
                && level.getBlockEntity(position)
                        instanceof EnergyInputChamberBlockEntity chamber) {
            chamber.handleUse(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
