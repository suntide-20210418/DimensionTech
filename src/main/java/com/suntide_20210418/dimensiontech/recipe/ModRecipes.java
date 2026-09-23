package com.suntide_20210418.dimensiontech.recipe;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe serializers owned by the mod. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, DimensionTechMod.MOD_ID);

    private ModRecipes() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DimensionTechMod.MOD_ID, path);
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
