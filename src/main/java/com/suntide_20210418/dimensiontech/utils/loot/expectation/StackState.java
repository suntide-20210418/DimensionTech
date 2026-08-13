package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Complete final stack state. Count is stored exactly once here. */
public final class StackState {
    private final ItemStack stack;
    private final CompoundTag serialized;

    public StackState(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack").copy();
        this.serialized = this.stack.serializeNBT().copy();
    }

    public ItemStack stack() {
        return stack.copy();
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
        return serialized.hashCode();
    }
}
