package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureReactorLayout;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureReactorScreen;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration for the structure reactor. JEI discovers this by the {@link JeiPlugin} annotation
 * and a public no-argument constructor, so the class must stay instantiable that way.
 *
 * <p>JEI rebuilds categories and recipes together whenever recipes are reloaded, which makes the
 * registration below the right place to read the live registry.
 */
@JeiPlugin
public final class StructureReactorJeiPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_UID =
            new ResourceLocation(DimensionTechMod.MOD_ID, "structure_reactor");

    public static final RecipeType<StructureReactorJeiRecipe> RECIPE_TYPE =
            RecipeType.create(
                    DimensionTechMod.MOD_ID, "structure_reactor", StructureReactorJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new StructureReactorJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                RECIPE_TYPE, StructureReactorJeiRecipes.build(StructureReactorRecipes.all()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(RECIPE_TYPE, ModBlocks.STRUCTURE_REACTOR.get());
    }

    /** Makes the reactor screen's recipe display a click target that opens this category. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
                StructureReactorScreen.class,
                StructureReactorLayout.RECIPE_DISPLAY.x(),
                StructureReactorLayout.RECIPE_DISPLAY.y(),
                StructureReactorLayout.RECIPE_DISPLAY.width(),
                StructureReactorLayout.RECIPE_DISPLAY.height(),
                RECIPE_TYPE);
    }
}
