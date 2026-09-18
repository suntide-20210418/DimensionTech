package com.suntide_20210418.dimensiontech.recipe;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Recipe serializers owned by the mod.
 *
 * <p>{@link EnchantmentMarkRecipe} declares {@code RecipeType.CRAFTING} as its type, so the vanilla
 * crafting menu picks it up even though its serializer is a custom one. That is the whole trick:
 * the mark economy is driven by NBT, which no static shapeless recipe can express.
 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DimensionTechMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<EnchantmentMarkRecipe>> ENCHANTMENT_MARK =
            SERIALIZERS.register("enchantment_mark", EnchantmentMarkRecipe.Serializer::new);

    private ModRecipes() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DimensionTechMod.MOD_ID, path);
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
