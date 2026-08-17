package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Complete final stack state. Count is stored exactly once here. */
public final class StackState {
    private final ItemStack stack;
    private final CompoundTag serialized;
    private final int hashCode;

    public StackState(ItemStack stack) {
        ItemStack source = Objects.requireNonNull(stack, "stack");
        // ItemStack.copy() intentionally returns EMPTY for count <= 0. Raw loot functions may
        // set a count to zero and then restore it later, so preserve the item and tag state while
        // taking the immutable snapshot.
        this.stack = copyPreservingCount(source);
        CompoundTag canonical = new CompoundTag();
        canonical.put("stack", source.serializeNBT().copy());
        // ItemStack.save() stores Count as a byte; keep the exact in-memory count as well.
        canonical.putInt("count", source.getCount());
        this.serialized = canonical;
        this.hashCode = serialized.hashCode();
    }

    public ItemStack stack() {
        return copyPreservingCount(stack);
    }

    public int count() {
        return stack.getCount();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof StackState other && serialized.equals(other.serialized);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return serialized.toString();
    }

    private static ItemStack copyPreservingCount(ItemStack source) {
        CompoundTag snapshot = source.serializeNBT().copy();
        ItemStack copy = ItemStack.of(snapshot);
        // The serialized Count field is a byte in vanilla; restore the exact runtime value.
        copy.setCount(source.getCount());
        return copy;
    }
}
