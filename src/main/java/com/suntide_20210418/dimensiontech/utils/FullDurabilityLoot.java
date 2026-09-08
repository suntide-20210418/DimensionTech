package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.loot.expectation.StackState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Applies the miner's full-durability output policy to realized and expected loot. */
public final class FullDurabilityLoot {
    private FullDurabilityLoot() {}

    public static List<ItemStack> normalize(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                result.add(normalize(stack));
            }
        }
        return List.copyOf(result);
    }

    public static StackMeasure normalize(StackMeasure measure) {
        StackMeasure result = new StackMeasure();
        for (var entry : measure.values().entrySet()) {
            result.add(new StackState(normalize(entry.getKey().stack())), entry.getValue());
        }
        return result;
    }

    public static ItemStack normalize(ItemStack stack) {
        ItemStack result = stack.copy();
        CompoundTag tag = result.getTag();
        if (result.getMaxDamage() > 0 && tag != null) {
            tag.remove("Damage");
            if (tag.isEmpty()) {
                result.setTag(null);
            }
        }
        return result;
    }
}
