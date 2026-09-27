package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItemTags;
import com.suntide_20210418.dimensiontech.item.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Item tags for the mod.
 *
 * <p>{@link ModItemTags#DIMENSION_FRAGMENTS} groups all six tiered fragments in one tag so a slot
 * can be gated on "a dimension fragment" while the recipes keep asking for the exact tier they
 * need.
 */
public class ModItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
    public ModItemTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(
                output,
                Registries.ITEM,
                lookupProvider,
                item -> item.builtInRegistryHolder().key(),
                DimensionTechMod.MOD_ID,
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var fragments = tag(ModItemTags.DIMENSION_FRAGMENTS);
        for (var fragment : ModItems.DIMENSION_FRAGMENTS) fragments.add(fragment.get());
    }
}
