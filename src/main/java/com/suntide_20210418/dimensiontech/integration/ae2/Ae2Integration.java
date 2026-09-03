package com.suntide_20210418.dimensiontech.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.misc.InterfaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class Ae2Integration {
    private static final IActionSource ACTION_SOURCE = IActionSource.empty();

    private Ae2Integration() {}

    public static boolean isOnlineInterface(BlockEntity blockEntity) {
        return blockEntity instanceof InterfaceBlockEntity interfaceBlockEntity
                && interfaceBlockEntity.getMainNode().isOnline();
    }

    public static ItemStack insertIntoInterfaceNetwork(BlockEntity blockEntity, ItemStack stack) {
        if (stack.isEmpty()
                || !(blockEntity instanceof InterfaceBlockEntity interfaceBlockEntity)
                || !interfaceBlockEntity.getMainNode().isOnline()) {
            return stack;
        }

        var grid = interfaceBlockEntity.getMainNode().getGrid();
        AEItemKey key = AEItemKey.of(stack);
        if (grid == null || key == null) {
            return stack;
        }

        long inserted =
                grid.getStorageService()
                        .getInventory()
                        .insert(key, stack.getCount(), Actionable.MODULATE, ACTION_SOURCE);
        if (inserted <= 0) {
            return stack;
        }
        if (inserted >= stack.getCount()) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink((int) inserted);
        return remainder;
    }

    /** Extracts from the ME network storage service, never from the interface's local inventory. */
    public static int extractFluidFromInterfaceNetwork(
            BlockEntity blockEntity, Fluid fluid, int requestedAmount, IFluidHandler destination) {
        if (requestedAmount <= 0
                || !(blockEntity instanceof InterfaceBlockEntity interfaceBlockEntity)
                || !interfaceBlockEntity.getMainNode().isOnline()) {
            return 0;
        }
        var grid = interfaceBlockEntity.getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        var key = AEFluidKey.of(fluid);
        int accepted =
                destination.fill(
                        new FluidStack(fluid, requestedAmount), IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }
        long extracted =
                grid.getStorageService()
                        .getInventory()
                        .extract(key, accepted, Actionable.MODULATE, ACTION_SOURCE);
        if (extracted <= 0) {
            return 0;
        }
        return destination.fill(
                new FluidStack(fluid, Math.toIntExact(Math.min(Integer.MAX_VALUE, extracted))),
                IFluidHandler.FluidAction.EXECUTE);
    }
}
