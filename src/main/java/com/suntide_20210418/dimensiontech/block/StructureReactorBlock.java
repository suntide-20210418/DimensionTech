package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.StructureReactorBlockEntity;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorAnalogSignal;
import com.suntide_20210418.dimensiontech.structurereactor.StateId;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

public final class StructureReactorBlock extends BaseEntityBlock {
    private static final String COMPARATOR_HINT =
            "tooltip.dimension_tech.structure_reactor.comparator";
    private static final String HOLD_SHIFT_HINT =
            "tooltip.dimension_tech.structure_reactor.hold_shift";
    private static final String COMPARATOR_STEPS =
            "tooltip.dimension_tech.structure_reactor.comparator.steps";
    private static final String COMPARATOR_LEVELS =
            "tooltip.dimension_tech.structure_reactor.comparator.levels";

    /** The horizontal direction the front (east, by convention) faces. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public StructureReactorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Places the front toward the player: the front direction is the opposite of the direction the
     * player is facing.
     */
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureReactorBlockEntity(pos, state);
    }

    /**
     * The reactor answers comparators, and only comparators.
     *
     * <p>{@code getSignal} and {@code getDirectSignal} are deliberately left at zero. Redstone
     * control reads {@code Level#getBestNeighborSignal}, which asks each neighbour for the signal
     * it points back with - so a reactor that emitted power would light the dust beside it, read
     * that dust back, and latch itself on with no way to switch it off.
     */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StructureReactorBlockEntity reactor
                ? reactor.analogSignal()
                : 0;
    }

    /**
     * Points players at the comparator interface. The level table sits behind shift because it is
     * reference material rather than a property of the block, mirroring the miner's build list.
     */
    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable BlockGetter level,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(
                Component.translatable(
                                COMPARATOR_HINT,
                                ReactorAnalogSignal.MIN_VALUE,
                                ReactorAnalogSignal.MAX_VALUE)
                        .withStyle(ChatFormatting.AQUA));
        if (!Screen.hasShiftDown()) {
            tooltip.add(
                    Component.translatable(HOLD_SHIFT_HINT).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.empty());
        tooltip.add(
                Component.translatable(
                                COMPARATOR_STEPS,
                                firstStepLevel(),
                                lastStepLevel(),
                                firstStepLevel() + ReactorAnalogSignal.REWARD_WINDOW_OFFSET,
                                lastStepLevel() + ReactorAnalogSignal.REWARD_WINDOW_OFFSET)
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                COMPARATOR_LEVELS,
                                ReactorAnalogSignal.IDLE,
                                ReactorAnalogSignal.IDLE_BLOCKED,
                                ReactorAnalogSignal.COMMIT_BLOCKED,
                                ReactorAnalogSignal.REFINING)
                        .withStyle(ChatFormatting.GRAY));
    }

    /**
     * The lowest and highest level an operation step announces, which the tooltip advertises as one
     * range instead of listing every step. The signal mapping test pins the steps to a contiguous
     * run, so that range is always exactly the steps and never includes a reserved level.
     */
    private static int firstStepLevel() {
        return StateId.values()[0].signalLevel();
    }

    private static int lastStepLevel() {
        StateId[] steps = StateId.values();
        return steps[steps.length - 1].signalLevel();
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StructureReactorBlockEntity reactor))
            return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        Optional<IFluidHandlerItem> container =
                held.getCapability(Capabilities.FluidHandler.ITEM).resolve();
        if (container.isPresent()) {
            if (!level.isClientSide) exchangeFluid(player, hand, held, reactor, container.get());
            // A held fluid container is spent on the transfer and never opens the screen, so the
            // interaction result stays the same on both sides.
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer server)
            NetworkHooks.openScreen(server, reactor, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Runs the tank transfer and then re-places the held container, because a container carries its
     * fluid in the item itself: a water bucket becomes an empty one and the reverse.
     */
    private static void exchangeFluid(
            Player player,
            InteractionHand hand,
            ItemStack held,
            StructureReactorBlockEntity reactor,
            IFluidHandlerItem container) {
        if (!reactor.exchangeWithFluidContainer(container)) return;
        ItemStack result = container.getContainer();
        if (result.isEmpty() || ItemStack.isSameItemSameTags(result, held)) return;
        if (held.getCount() == 1) {
            player.setItemInHand(hand, result);
        } else if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (!player.getInventory().add(result)) player.drop(result, false);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        com.suntide_20210418.dimensiontech.block.entity.ModBlockEntities
                                .STRUCTURE_REACTOR
                                .get(),
                        StructureReactorBlockEntity::serverTick);
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof StructureReactorBlockEntity reactor) {
            reactor.abortAndReturnResources();
            reactor.dropContents();
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
