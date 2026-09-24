package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.loot.expectation.StackState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
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
        /*
         * 1.20.1 的写法是从根 tag 里删掉 "Damage"，只有在删完后 tag 变空时才顺手 setTag(null)。也就是说
         * 附魔（"Enchantments"）与修复成本（"RepairCost"）从来没被清掉过：只要 tag 里还有它们，tag 就非空，
         * setTag(null) 根本不会执行。1.21 里耐久、附魔、修复成本各自是独立组件，所以这里只移除承载耐久的那一个
         * （DataComponents.DAMAGE，缺失即满耐久），不碰 ENCHANTMENTS / REPAIR_COST —— 否则会把掉落物的附魔
         * 一起洗掉，那是行为变更而不是移植。
         */
        if (result.getMaxDamage() > 0) {
            result.remove(DataComponents.DAMAGE);
        }
        return result;
    }
}
