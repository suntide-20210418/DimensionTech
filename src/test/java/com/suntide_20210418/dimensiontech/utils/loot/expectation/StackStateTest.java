package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

class StackStateTest {
    @Test
    void zeroCountRetainsItemForLaterFunctions() {
        ItemStack input = new ItemStack(Items.STONE);
        input.setCount(0);

        StackState state = new StackState(input);

        assertEquals(Items.STONE, state.stack().getItem());
        assertEquals(0, state.count());
    }

    @Test
    void equalityRetainsCountsOutsideTheNbtByteRange() {
        StackState lower = new StackState(new ItemStack(Items.STONE, 72));
        StackState higher = new StackState(new ItemStack(Items.STONE, 200));

        assertEquals(72, lower.count());
        assertEquals(200, higher.count());
        org.junit.jupiter.api.Assertions.assertNotEquals(lower, higher);
    }
}
