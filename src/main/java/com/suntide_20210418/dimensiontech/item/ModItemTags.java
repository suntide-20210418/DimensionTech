package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The item tags this mod defines.
 *
 * <p>The membership itself is written by {@code datagen/ModItemTagsProvider}, so the tag and the
 * items can never drift apart: both sides read the same registry objects.
 */
public final class ModItemTags {
    /**
     * Every dimension fragment, any tier, so a machine can say "a fragment goes here" without
     * listing the six items. Which tier is actually usable stays the recipe's business - the
     * reactor still checks the fragment against the recipe it is about to start.
     */
    public static final TagKey<Item> DIMENSION_FRAGMENTS = tag("dimension_fragments");

    private static TagKey<Item> tag(String path) {
        return ItemTags.create(ResourceLocationHelper.modLoc(path));
    }

    private ModItemTags() {}
}
