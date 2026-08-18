package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The observable terminal state used by structure valuation.
 *
 * <p>This key is only valid after every loot function has run. It deliberately discards state that
 * can affect a later function, while retaining the runtime rarity that valuation observes.
 */
public record TerminalStackKey(Item item, int count, Rarity rarity) {
    public TerminalStackKey {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(rarity, "rarity");
    }

    /** Captures all state observed by terminal item-count and rarity valuation. */
    public static TerminalStackKey from(StackState state) {
        Objects.requireNonNull(state, "state");
        return from(state.stack());
    }

    /** Captures {@link ItemStack#getRarity()} before any non-observable state is discarded. */
    public static TerminalStackKey from(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return new TerminalStackKey(stack.getItem(), stack.getCount(), stack.getRarity());
    }
}
