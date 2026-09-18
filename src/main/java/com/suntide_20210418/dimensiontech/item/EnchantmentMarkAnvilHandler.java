package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.loot.expectation.EnchantmentKey;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Applies a mark to an item or a book in the anvil, exactly like an enchanted book.
 *
 * <p>Only the right slot may hold a mark, so a mark is always the material being spent and never
 * the target. The output level is clamped to {@link Enchantment#getMaxLevel()}: unlike vanilla
 * book stacking, this path cannot push an enchantment past its maximum.
 */
public final class EnchantmentMarkAnvilHandler {
    private EnchantmentMarkAnvilHandler() {}

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty()) return;
        Optional<EnchantmentKey> key = EnchantmentMarkItem.getKey(right);
        if (key.isEmpty() || EnchantmentMarkItem.getKey(left).isPresent()) return;

        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(key.get().enchantment());
        if (enchantment == null) return;
        int max = enchantment.getMaxLevel();
        int level = Math.min(max, key.get().level());
        if (level <= 0) return;

        boolean book = left.is(Items.BOOK);
        if (!book && !enchantment.canApplyAtEnchantingTable(left) && !enchantment.canEnchant(left)) {
            return;
        }

        ItemStack target = book ? new ItemStack(Items.ENCHANTED_BOOK) : left.copy();
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(target);
        for (Enchantment present : existing.keySet()) {
            if (present != enchantment && !enchantment.isCompatibleWith(present)) return;
        }
        int current = existing.getOrDefault(enchantment, 0);
        if (current >= level) return;
        int applied = Math.min(max, Math.max(current, level));

        if (book) {
            EnchantedBookItem.addEnchantment(
                    target, new EnchantmentInstance(enchantment, applied));
        } else {
            target.enchant(enchantment, applied);
        }
        event.setOutput(target);
        event.setCost(Math.max(1, applied));
        event.setMaterialCost(1);
    }
}
