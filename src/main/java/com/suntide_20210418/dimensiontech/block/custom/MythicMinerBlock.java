package com.suntide_20210418.dimensiontech.block.custom;

import com.suntide_20210418.dimensiontech.block.entity.MythicMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public abstract class MythicMinerBlock extends BaseEntityBlock {

    protected MythicMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
            BlockState blockState,
            Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof MythicMinerBlockEntity blockEntity) {
            blockEntity.drawMarkerLoot(serverPlayer.server);
            NetworkHooks.openScreen(serverPlayer, blockEntity, position);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
