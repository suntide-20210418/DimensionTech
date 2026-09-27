package com.suntide_20210418.dimensiontech.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A slot view automation may only feed.
 *
 * <p>The machines here hold material they consume: markers the miner reads, fragments and operation
 * items the reactor cycles through. Automation — pipes, storage buses, the ME network — has to be
 * able to load those slots and must never be able to empty them, because one extraction takes the
 * very stack a running cycle is waiting on. Each machine therefore keeps its own {@code
 * ItemStackHandler} for its menu and its own logic, and hands out this shape instead: inserts and
 * reads pass through to the real handler, so {@code isItemValid} and slot limits still decide what
 * may be loaded, while extraction always reports empty.
 */
final class InsertOnlyItemHandler implements IItemHandler {
    private final IItemHandler delegate;

    InsertOnlyItemHandler(IItemHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return delegate.isItemValid(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }
}
