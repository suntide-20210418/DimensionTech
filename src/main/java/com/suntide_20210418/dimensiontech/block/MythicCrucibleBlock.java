package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.network.NetworkHooks;

public final class MythicCrucibleBlock extends BaseEntityBlock {
    public MythicCrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MythicCrucibleBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MythicCrucibleBlockEntity crucible))
            return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        Optional<IFluidHandlerItem> container =
                held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (container.isPresent()) {
            if (!level.isClientSide) exchangeFluid(player, hand, held, crucible, container.get());
            // A held fluid container is spent on the transfer and never opens the screen, so the
            // interaction result stays the same on both sides.
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer server)
            NetworkHooks.openScreen(server, crucible, pos);
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
            MythicCrucibleBlockEntity crucible,
            IFluidHandlerItem container) {
        if (!crucible.exchangeWithFluidContainer(container)) return;
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
                                .MYTHIC_CRUCIBLE
                                .get(),
                        MythicCrucibleBlockEntity::serverTick);
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof MythicCrucibleBlockEntity crucible) {
            crucible.abortAndReturnResources();
            crucible.dropContents();
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
