package com.suntide_20210418.dimensiontech.utils;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The item view a machine hands to automation: the same slots, insert-only.
 *
 * <p>Every slot these machines expose holds material the machine itself runs on - the marker a
 * structure miner reads, the fragment and operation items the reactor's cycle pays with - so an
 * extractor (a hopper, a transporter, a pipe in pull mode) must not be able to take any of it.
 * Handing out the backing {@link net.minecraftforge.items.ItemStackHandler} hands out exactly that
 * ability, because {@code extractItem} on it succeeds. This view forwards everything except
 * extraction, which always reports empty.
 *
 * <p>The player keeps full access: menus bind their slots to the backing handler, so items are
 * still picked up by hand, and the machine's own code keeps reading the handler it owns.
 */
public final class InsertOnlyItemHandler implements IItemHandler {
    private final IItemHandler backing;

    public InsertOnlyItemHandler(IItemHandler backing) {
        this.backing = backing;
    }

    @Override
    public int getSlots() {
        return backing.getSlots();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return backing.getStackInSlot(slot);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return backing.insertItem(slot, stack, simulate);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return backing.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return backing.isItemValid(slot, stack);
    }
}
