package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Complete final stack state. Count is stored exactly once here. */
public final class StackState {
    private final CompoundTag serialized;
    private final int hashCode;

    public StackState(ItemStack stack) {
        ItemStack source = Objects.requireNonNull(stack, "stack");
        CompoundTag canonical = new CompoundTag();
        canonical.put("stack", serializePreservingCount(source));
        // ItemStack.save() stores Count as a byte; keep the exact in-memory count as well.
        canonical.putInt("count", source.getCount());
        this.serialized = canonical.copy();
        this.hashCode = this.serialized.hashCode();
    }

    private StackState(CompoundTag serialized) {
        this.serialized = serialized.copy();
        this.hashCode = this.serialized.hashCode();
    }

    public ItemStack stack() {
        ItemStack copy = ItemStack.of(serialized.getCompound("stack").copy());
        copy.setCount(count());
        return copy;
    }

    public int count() {
        return serialized.getInt("count");
    }

    /** Detached serialized payload; the normalized vanilla Count byte is restored separately. */
    public String serializedStackData() {
        return serialized.getCompound("stack").toString();
    }

    /** Changes only the explicit count while retaining the prior item's canonical state. */
    public StackState withCount(int count) {
        CompoundTag changed = serialized.copy();
        changed.putInt("count", count);
        return new StackState(changed);
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

    private static CompoundTag serializePreservingCount(ItemStack source) {
        ItemStack serializable = source.copy();
        // The exact count is deliberately stored only in the outer key. Normalize the nested
        // vanilla Count byte to retain item, tag, and Forge stack data without duplicating count.
        if (serializable.isEmpty()) {
            serializable.setCount(1);
        }
        CompoundTag serialized = serializable.serializeNBT().copy();
        serialized.putByte("Count", (byte) 1);
        return serialized;
    }
}
