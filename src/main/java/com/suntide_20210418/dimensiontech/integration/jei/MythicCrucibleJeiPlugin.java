package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleLayout;
import com.suntide_20210418.dimensiontech.client.gui.screen.MythicCrucibleScreen;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration for the mythic crucible. JEI discovers this by the {@link JeiPlugin} annotation
 * and a public no-argument constructor, so the class must stay instantiable that way.
 *
 * <p>JEI rebuilds categories and recipes together whenever recipes are reloaded, which makes the
 * registration below the right place to read the live registry.
 */
@JeiPlugin
public final class MythicCrucibleJeiPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(DimensionTechMod.MOD_ID, "mythic_crucible");

    public static final RecipeType<MythicCrucibleJeiRecipe> RECIPE_TYPE =
            RecipeType.create(
                    DimensionTechMod.MOD_ID, "mythic_crucible", MythicCrucibleJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MythicCrucibleJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                RECIPE_TYPE, MythicCrucibleJeiRecipes.build(MythicCrucibleRecipes.all()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(RECIPE_TYPE, ModBlocks.MYTHIC_CRUCIBLE.get());
    }

    /** Makes the crucible screen's recipe display a click target that opens this category. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
                MythicCrucibleScreen.class,
                MythicCrucibleLayout.RECIPE_DISPLAY.x(),
                MythicCrucibleLayout.RECIPE_DISPLAY.y(),
                MythicCrucibleLayout.RECIPE_DISPLAY.width(),
                MythicCrucibleLayout.RECIPE_DISPLAY.height(),
                RECIPE_TYPE);
    }
}
