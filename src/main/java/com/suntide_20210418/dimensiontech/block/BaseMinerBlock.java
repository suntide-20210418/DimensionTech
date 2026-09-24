package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

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
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ModConfigs.StructureMinerTierConfig config =
                ModConfigs.TIERS[
                        Math.max(0, Math.min(ModConfigs.TIERS.length - 1, minerTier() - 1))];
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.structure_miner.base_parallel",
                                config.baseParallel())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.structure_miner.efficiency",
                                config.efficiency())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.structure_miner.luck", config.baseLuck())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.structure_miner.energy_capacity",
                                config.energyCapacity())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.structure_miner.energy_consumption",
                                config.energyConsumption())
                        .withStyle(ChatFormatting.AQUA));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.structure_miner.marker_slots",
                                config.slotCount())
                        .withStyle(ChatFormatting.AQUA));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.empty());
            tooltip.add(
                    Component.translatable("tooltip.dimension_tech.structure_miner.materials")
                            .withStyle(ChatFormatting.GOLD));
            // Counted straight off the pattern, so the list can never drift from the coordinates.
            int[] materials = StructureMinerMultiblock.projectionCounts();
            addMaterialLine(
                    tooltip,
                    materials,
                    StructureMinerMultiblock.ProjectionKind.CASING,
                    "material.casing");
            addMaterialLine(
                    tooltip,
                    materials,
                    StructureMinerMultiblock.ProjectionKind.STRUCTURE,
                    "material.structure");
            addMaterialLine(
                    tooltip,
                    materials,
                    StructureMinerMultiblock.ProjectionKind.GLASS,
                    "material.glass");
            addMaterialLine(
                    tooltip,
                    materials,
                    StructureMinerMultiblock.ProjectionKind.UPGRADE,
                    "material.upgrade");
        } else {
            tooltip.add(
                    Component.translatable("tooltip.dimension_tech.structure_miner.hold_shift")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** One material line, skipped when the pattern does not use that block at all. */
    private static void addMaterialLine(
            List<Component> tooltip,
            int[] materials,
            StructureMinerMultiblock.ProjectionKind kind,
            String langKey) {
        int count = materials[kind.ordinal()];
        if (count <= 0) return;
        tooltip.add(
                Component.translatable("tooltip.dimension_tech.structure_miner." + langKey, count)
                        .withStyle(ChatFormatting.GRAY));
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
    public ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState blockState,
            Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (stack.is(ModItems.WRENCH.get())) {
            // Shift+right-click charges from the player's inventory and builds the multiblock;
            // plain right-click only toggles the projection overlay.
            if (player.isShiftKeyDown()) {
                if (level instanceof ServerLevel serverLevel) {
                    Component message =
                            StructureMinerMultiblock.buildFromInventory(
                                    serverLevel, position, player);
                    if (message != null) {
                        player.displayClientMessage(message, true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.suntide_20210418.dimensiontech.client.StructureMinerProjectionClient.toggle(
                        position);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        IFluidHandlerItem container = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (container != null) {
            if (!level.isClientSide()
                    && level.getBlockEntity(position) instanceof BaseMinerBlockEntity miner) {
                exchangeFluid(player, hand, stack, miner, container);
            }
            // A held fluid container is spent on the transfer and never opens the screen, so the
            // interaction result stays the same on both sides.
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof BaseMinerBlockEntity blockEntity) {
            // The menu factory rebuilds the block entity from the position it reads first.
            serverPlayer.openMenu(blockEntity, buffer -> buffer.writeBlockPos(position));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Runs the tank transfer and then re-places the held container, because a container carries its
     * fluid in the item itself: a water bucket becomes an empty one and the reverse.
     */
    private static void exchangeFluid(
            Player player,
            InteractionHand hand,
            ItemStack held,
            BaseMinerBlockEntity miner,
            IFluidHandlerItem container) {
        if (!miner.exchangeWithFluidContainer(container)) return;
        ItemStack result = container.getContainer();
        if (result.isEmpty() || ItemStack.isSameItemSameComponents(result, held)) return;
        if (held.getCount() == 1) {
            player.setItemInHand(hand, result);
        } else if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (!player.getInventory().add(result)) player.drop(result, false);
        }
    }

    /**
     * Returns the machine's contents to the world when the block itself goes away.
     *
     * <p>The guard mirrors {@code StructureReactorBlock}: a replacement that is the same block (a
     * facing change, for instance) must not spill the inventory, because the block entity survives
     * that transition.
     */
    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos position,
            BlockState replacement,
            boolean moving) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(position) instanceof BaseMinerBlockEntity miner) {
            miner.dropContents();
        }
        super.onRemove(state, level, position, replacement, moving);
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
