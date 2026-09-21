package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The deconstruction-core's own JEI page: how it is earned. This page stands apart from the per-tier
 * miner pages and spells out the full source story — the mining-drop chance range, the chest pity,
 * the chest-loot path before a first miner exists, and the craft it feeds.
 *
 * <p>The core icon is centred and the full explanation is rendered as wrapped text directly on the
 * page, so every line is visible without a tooltip. The core is placed as the recipe's only output
 * slot, so JEI's "how it is obtained" lookup for the core leads here alongside the per-tier pages.
 */
public final class DeconstructionCoreJeiCategory
        extends AbstractRecipeCategory<DeconstructionCoreJeiRecipe> {
    private static final int WIDTH = 184;
    private static final int HEIGHT = 100;

    private static final int ICON = 16;
    private static final int ICON_X = (WIDTH - ICON) / 2;
    private static final int ICON_Y = 4;

    private static final int TEXT_X = 2;
    private static final int TEXT_Y = 26;

    public DeconstructionCoreJeiCategory(IGuiHelper guiHelper) {
        super(
                StructureMinerJeiPlugin.CORE_TYPE,
                Component.translatable(
                        TranslateHelper.item("dimension_deconstruction_core")),
                guiHelper.createDrawableItemLike(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()),
                WIDTH,
                HEIGHT);
    }

    @Override
    public ResourceLocation getRegistryName(DeconstructionCoreJeiRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath("dimension_tech", "deconstruction_core");
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder, DeconstructionCoreJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, ICON_X, ICON_Y)
                .addItemStack(new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()));
    }

    @Override
    public void createRecipeExtras(
            IRecipeExtrasBuilder builder, DeconstructionCoreJeiRecipe recipe, IFocusGroup focuses) {
        // The whole explanation wraps onto its own lines inside the reserved box, so no line is ever
        // clipped into a tooltip — none of it is hidden.
        builder.addText(
                        StructureMinerJeiText.sourcePageLines(recipe),
                        WIDTH - TEXT_X * 2,
                        HEIGHT - TEXT_Y - 4)
                .setPosition(TEXT_X, TEXT_Y)
                .setLineSpacing(1);
    }

    @Override
    public void draw(
            DeconstructionCoreJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY) {
        // The centred core icon and the wrapped explanation define the page; nothing else is drawn.
    }
}