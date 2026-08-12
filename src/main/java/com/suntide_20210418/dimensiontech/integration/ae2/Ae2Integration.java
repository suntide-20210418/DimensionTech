package com.suntide_20210418.dimensiontech.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.misc.InterfaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

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
}
