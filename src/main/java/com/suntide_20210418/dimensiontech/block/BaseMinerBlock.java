package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;

public abstract class BaseMinerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public int minerTier() {
        return 1;
    }

    protected BaseMinerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable BlockGetter level,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ModConfigs.MythicMinerTierConfig config =
                ModConfigs.TIERS[
                        Math.max(0, Math.min(ModConfigs.TIERS.length - 1, minerTier() - 1))];
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.mythic_miner.base_parallel",
                                config.baseParallel())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.mythic_miner.efficiency",
                                config.efficiency())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.mythic_miner.luck", config.baseLuck())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.mythic_miner.energy_capacity",
                                config.energyCapacity())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.mythic_miner.energy_consumption",
                                config.energyConsumption())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.mythic_miner.marker_slots",
                                config.slotCount())
                        .withStyle(ChatFormatting.AQUA));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.empty());
            tooltip.add(
                    Component.translatable("tooltip.dimension_tech.mythic_miner.materials")
                            .withStyle(ChatFormatting.GOLD));
            tooltip.add(
                    Component.translatable(
                                    "tooltip.dimension_tech.mythic_miner.material.casing", 32)
                            .withStyle(ChatFormatting.GRAY));
            tooltip.add(
                    Component.translatable(
                                    "tooltip.dimension_tech.mythic_miner.material.structure", 1)
                            .withStyle(ChatFormatting.GRAY));
            tooltip.add(
                    Component.translatable(
                                    "tooltip.dimension_tech.mythic_miner.material.focus",
                                    minerTier(),
                                    8)
                            .withStyle(ChatFormatting.GRAY));
            tooltip.add(
                    Component.translatable(
                                    "tooltip.dimension_tech.mythic_miner.material.upgrade", 12)
                            .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(
                    Component.translatable("tooltip.dimension_tech.mythic_miner.hold_shift")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        if (player.getItemInHand(hand).is(Items.STICK)) {
            if (level.isClientSide()) {
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () ->
                                () ->
                                        com.suntide_20210418.dimensiontech.client
                                                .MythicMinerProjectionClient.toggle(
                                                position, minerTier()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof BaseMinerBlockEntity blockEntity) {
            NetworkHooks.openScreen(serverPlayer, blockEntity, position);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (tickerLevel, position, state, blockEntity) -> {
            if (blockEntity instanceof BaseMinerBlockEntity miner) {
                miner.serverTick();
            }
        };
    }
}
