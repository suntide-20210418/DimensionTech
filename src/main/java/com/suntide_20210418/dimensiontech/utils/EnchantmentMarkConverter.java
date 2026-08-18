package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.EnchantmentMarkItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Converts realized enchanted loot into marks without changing loot-table execution semantics. */
public final class EnchantmentMarkConverter {
    private static final long MAX_MATERIALIZED_MARKS = 65_536L;

    private EnchantmentMarkConverter() {}

    public static List<ItemStack> convert(List<ItemStack> loot) {
        List<ItemStack> unchanged = new ArrayList<>();
        Map<ResourceLocation, Long> markCounts = new LinkedHashMap<>();
        for (ItemStack stack : loot) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!hasEnchantments(stack) || !convertStack(stack, markCounts)) {
                unchanged.add(stack.copy());
            }
        }

        markCounts.forEach(
                (enchantmentId, count) -> appendMarkStacks(unchanged, enchantmentId, count));
        return List.copyOf(unchanged);
    }

    private static boolean hasEnchantments(ItemStack stack) {
        if (stack.isEnchanted()) {
            return true;
        }
        return stack.is(Items.ENCHANTED_BOOK)
                && stack.hasTag()
                && !stack.getTag().getList("StoredEnchantments", Tag.TAG_COMPOUND).isEmpty();
    }

    private static boolean convertStack(ItemStack stack, Map<ResourceLocation, Long> markCounts) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) {
            return false;
        }

        Map<ResourceLocation, Long> stackCounts = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            int level = entry.getValue();
            if (enchantmentId == null || level <= 0 || level >= Long.SIZE - 1) {
                DimensionTechMod.LOGGER.warn(
                        "Keeping enchanted loot unchanged because enchantment {} level {} cannot be"
                                + " safely materialized as marks",
                        enchantmentId,
                        level);
                return false;
            }
            long marksPerItem = 1L << level;
            if (marksPerItem > MAX_MATERIALIZED_MARKS / stack.getCount()) {
                DimensionTechMod.LOGGER.warn(
                        "Keeping enchanted loot unchanged because enchantment {} level {} would"
                                + " create more than {} marks",
                        enchantmentId,
                        level,
                        MAX_MATERIALIZED_MARKS);
                return false;
            }
            long count = (long) stack.getCount() * marksPerItem;
            stackCounts.merge(enchantmentId, count, Math::addExact);
        }

        Map<ResourceLocation, Long> combinedCounts = new LinkedHashMap<>(markCounts);
        try {
            stackCounts.forEach(
                    (enchantmentId, count) ->
                            combinedCounts.merge(enchantmentId, count, Math::addExact));
        } catch (ArithmeticException exception) {
            DimensionTechMod.LOGGER.warn(
                    "Keeping enchanted loot unchanged because its enchantment mark count"
                            + " overflowed",
                    exception);
            return false;
        }
        long totalMarks = 0L;
        for (long count : combinedCounts.values()) {
            totalMarks = Math.addExact(totalMarks, count);
            if (totalMarks > MAX_MATERIALIZED_MARKS) {
                DimensionTechMod.LOGGER.warn(
                        "Keeping enchanted loot unchanged because this draw would create more than"
                                + " {} marks",
                        MAX_MATERIALIZED_MARKS);
                return false;
            }
        }
        markCounts.clear();
        markCounts.putAll(combinedCounts);
        return true;
    }

    private static void appendMarkStacks(
            List<ItemStack> output, ResourceLocation enchantmentId, long count) {
        int maxStackSize = 64;
        while (count > 0) {
            int stackSize = (int) Math.min(count, maxStackSize);
            output.add(EnchantmentMarkItem.create(enchantmentId, stackSize));
            count -= stackSize;
        }
    }
}
