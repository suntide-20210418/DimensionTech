package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** The complete terminal observation used by structure-value calculation. */
public record StackObservation(Item item, int count, Rarity rarity) {
    public StackObservation {
        item = Objects.requireNonNull(item, "item");
        rarity = Objects.requireNonNull(rarity, "rarity");
    }

    public static StackObservation observe(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return new StackObservation(stack.getItem(), stack.getCount(), stack.getRarity());
    }
}
