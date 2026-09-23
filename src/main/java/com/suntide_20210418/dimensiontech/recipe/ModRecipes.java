package com.suntide_20210418.dimensiontech.recipe;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Recipe serializers owned by the mod. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DimensionTechMod.MOD_ID);

    private ModRecipes() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(DimensionTechMod.MOD_ID, path);
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
