package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Item tags. Only the tags the mod itself reads are emitted here; anything a pack or another mod
 * wants to add is meant to be added on top of these from its own data pack.
 */
public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries,
            @Nullable ExistingFileHelper existingFileHelper) {
        // No block tags feed this provider, so the block-tag lookup it would copy from is empty.
        super(
                output,
                registries,
                CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                DimensionTechMod.MOD_ID,
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var fragments = tag(ModItems.DIMENSION_FRAGMENTS_TAG);
        for (DeferredHolder<Item, Item> fragment : ModItems.DIMENSION_FRAGMENTS) {
            fragments.add(fragment.get());
        }
    }
}
