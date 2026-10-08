package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The one place the display layer asks what an item is worth.
 *
 * <p>The multiplier is the reason this exists. It is a config-backed value with a regex override
 * list on top, so it is a lookup rather than arithmetic — and it was previously defined inside the
 * operator's operate page, which meant every other screen reached into a peer page class for a
 * number that has nothing to do with that page.
 *
 * <p><b>Do not call this per frame.</b> Each call walks the override list and compiles a pattern
 * per entry. Compute it once when the row list is rebuilt and carry the result.
 */
public final class ItemValueFacade {
    private ItemValueFacade() {}

    /** The authoritative per-item multiplier, from config with overrides applied. */
    public static double multiplier(Item item) {
        return ModConfigs.STRUCTURE_VALUE.itemMultiplier(item, new ItemStack(item).getRarity());
    }
}
