package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * One enchantment type at one concrete level.
 *
 * <p>The identity used by the enchantment-mark economy. Two marks combine only when both components
 * match, which is exactly what record equality gives here.
 */
public record EnchantmentKey(ResourceLocation enchantment, int level) {
    public EnchantmentKey {
        Objects.requireNonNull(enchantment, "enchantment");
        if (level <= 0) {
            throw new IllegalArgumentException("enchantment level must be positive: " + level);
        }
    }

    @Override
    public String toString() {
        return enchantment + "@" + level;
    }
}
