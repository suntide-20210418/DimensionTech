package com.suntide_20210418.dimensiontech.block;

import com.mojang.serialization.MapCodec;
import com.suntide_20210418.dimensiontech.block.entity.FluidInputChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

/**
 * The fluid input chamber: the miner's single 16,000 mB tank, moved out of the controller and into
 * a casing block.
 *
 * <p>Plain right-click reports the stored amount in chat; shift+right-click toggles the chamber's
 * auto-pull. A held fluid container is spent on a direct transfer instead, exactly as the
 * controller used to accept one, so a bucket still works without opening anything.
 */
public final class FluidInputChamberBlock extends MinerChamberBlock {
    public static final MapCodec<FluidInputChamberBlock> CODEC =
            simpleCodec(FluidInputChamberBlock::new);

    public FluidInputChamberBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(BlockPos position, BlockState blockState) {
        return new FluidInputChamberBlockEntity(position, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState blockState,
            Level level,
            BlockPos position,
            Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide
                && level.getBlockEntity(position) instanceof FluidInputChamberBlockEntity chamber) {
            chamber.handleUse(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState blockState,
            Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        // Only a held fluid container is claimed here. Anything else falls through so the player
        // can
        // still place blocks against the chamber.
        IFluidHandlerItem container = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (container == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide
                && level.getBlockEntity(position) instanceof FluidInputChamberBlockEntity chamber) {
            exchangeFluid(player, hand, stack, chamber, container);
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
            FluidInputChamberBlockEntity chamber,
            IFluidHandlerItem container) {
        if (!chamber.exchangeWithFluidContainer(container)) return;
        ItemStack result = container.getContainer();
        if (result.isEmpty() || ItemStack.isSameItemSameComponents(result, held)) return;
        if (held.getCount() == 1) {
            player.setItemInHand(hand, result);
        } else if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (!player.getInventory().add(result)) player.drop(result, false);
        }
    }
}
