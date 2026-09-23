package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * One miner tier's JEI page. The pages are split one-per-tier (a tier's page shows only that tier's
 * recipe), which is why this category is constructed with a concrete tier, recipe type and icon.
 *
 * The slot row is centred and carries the tier's real inputs (essence plus a target marker) and
 * its products (this tier's fragments, tokens, the structure's own loot and the deconstruction-core
 * chance). Each slot's explanation lives in that slot's tooltip; only the arrow keeps an on-hover
 * note about the time algorithm.
 */
public final class StructureMinerJeiCategory
        extends AbstractRecipeCategory<StructureMinerJeiRecipe> {
    private static final int WIDTH = 184;
    private static final int HEIGHT = 30;

    private static final int TOP_Y = 6;
    private static final int SLOT = 16;
    private static final int STRIDE = 20;

    // Elements left to right: fluid, marker, [arrow], fragment, token, loot, core.
    private static final int START_X = (WIDTH - (7 * STRIDE - 4)) / 2;
    private static final int FLUID_X = START_X;
    private static final int MARKER_X = FLUID_X + STRIDE;
    private static final int ARROW_X = MARKER_X + STRIDE;
    private static final int FRAGMENT_X = ARROW_X + STRIDE;
    private static final int TOKEN_X = FRAGMENT_X + STRIDE;
    private static final int LOOT_X = TOKEN_X + STRIDE;
    private static final int CORE_X = LOOT_X + STRIDE;

    private final int tier;

    public StructureMinerJeiCategory(
            IGuiHelper guiHelper, int tier, RecipeType<StructureMinerJeiRecipe> recipeType, Block icon) {
        super(
                recipeType,
                Component.translatable(TranslateHelper.block("tier_" + tier + "_structure_miner")),
                guiHelper.createDrawableItemLike(icon),
                WIDTH,
                HEIGHT);
        this.tier = tier;
    }

    /** The tier is the recipe's identity; the chest variant gets its own name so both bookmark. */
    @Override
    public ResourceLocation getRegistryName(StructureMinerJeiRecipe recipe) {
        return new ResourceLocation(
                "dimension_tech",
                recipe.chestVariant()
                        ? "chest_miner/tier_" + tier
                        : "structure_miner/tier_" + tier);
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder, StructureMinerJeiRecipe recipe, IFocusGroup focuses) {
        if (isRenderable(recipe.inputFluid())) {
            builder.addSlot(RecipeIngredientRole.INPUT, FLUID_X, TOP_Y)
                    .addFluidStack(recipe.inputFluid(), recipe.fluidPerCycleMb())
                    .setFluidRenderer(recipe.fluidPerCycleMb(), false, SLOT, SLOT);
        }
        if (recipe.chestVariant()) {
            builder.addSlot(RecipeIngredientRole.INPUT, MARKER_X, TOP_Y)
                    .addItemStack(chestMarker())
                    .addRichTooltipCallback(
                            (view, tooltip) -> {
                                tooltip.add(ChestMinerJeiText.chestMarkerTip());
                                tooltip.add(StructureMinerJeiText.notConsumed());
                            });
        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, MARKER_X, TOP_Y)
                    .addItemStack(markedMarker())
                    .addRichTooltipCallback(
                            (view, tooltip) -> {
                                tooltip.add(StructureMinerJeiText.markerTip());
                                tooltip.add(StructureMinerJeiText.notConsumed());
                            });
        }
        ItemStack fragment = fragment(recipe);
        if (!fragment.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, FRAGMENT_X, TOP_Y)
                    .addItemStack(fragment)
                    .addRichTooltipCallback(
                            (view, tooltip) ->
                                    tooltip.add(StructureMinerJeiText.productTip(recipe)));
        }
        ItemStack token = token(recipe);
        if (!token.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, TOKEN_X, TOP_Y)
                    .addItemStack(token)
                    .addRichTooltipCallback(
                            (view, tooltip) ->
                                    tooltip.add(StructureMinerJeiText.productTip(recipe)));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, LOOT_X, TOP_Y)
                .addItemStack(recipe.chestVariant() ? chestLoot() : structureLoot())
                .addRichTooltipCallback(
                        (view, tooltip) -> {
                            tooltip.add(
                                    recipe.chestVariant()
                                            ? ChestMinerJeiText.chestLootTip()
                                            : StructureMinerJeiText.lootTip());
                            tooltip.add(StructureMinerJeiText.rewardNote());
                        });
        builder.addSlot(RecipeIngredientRole.OUTPUT, CORE_X, TOP_Y)
                .addItemStack(new ItemStack(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()))
                .addRichTooltipCallback(
                        (view, tooltip) ->
                                StructureMinerJeiText.coreChanceTip(recipe).forEach(tooltip::add));
    }

    @Override
    public void createRecipeExtras(
            IRecipeExtrasBuilder builder, StructureMinerJeiRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(ARROW_X, TOP_Y + 1);
    }

    /** Hovering the arrow explains how structure value becomes the work-cycle duration. */
    @Override
    public void getTooltip(
            ITooltipBuilder tooltip,
            StructureMinerJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        if (insideArrow(mouseX, mouseY)) {
            StructureMinerJeiText.timeLines(BaseMinerBlockEntity.MINIMUM_PROCESSING_TIME)
                    .forEach(tooltip::add);
        }
    }

    private static boolean insideArrow(double mouseX, double mouseY) {
        return mouseX >= ARROW_X && mouseX < ARROW_X + 16 && mouseY >= TOP_Y && mouseY < TOP_Y + 16;
    }

    /** A target marker, named so the player knows the slot wants an already-marked one. */
    private static ItemStack markedMarker() {
        ItemStack stack = new ItemStack(ModItems.STRUCTURE_MARKER.get());
        stack.setHoverName(StructureMinerJeiText.markerName());
        return stack;
    }

    /** A chest marker, named so the player knows the slot wants an already-marked one. */
    private static ItemStack chestMarker() {
        ItemStack stack = new ItemStack(ModItems.CHEST_MARKER.get());
        stack.setHoverName(ChestMinerJeiText.chestMarkerName());
        return stack;
    }

    /** A Heart of the Sea standing in for the structure's own natural loot. */
    private static ItemStack structureLoot() {
        ItemStack stack = new ItemStack(Items.HEART_OF_THE_SEA);
        stack.setHoverName(StructureMinerJeiText.lootName());
        return stack;
    }

    /** A chest standing in for the marked chest's own loot expectations. */
    private static ItemStack chestLoot() {
        ItemStack stack = new ItemStack(Items.CHEST);
        stack.setHoverName(ChestMinerJeiText.chestLootName());
        return stack;
    }

    /** The tier's fragment rendered at the tier's per-cycle output count. */
    private static ItemStack fragment(StructureMinerJeiRecipe recipe) {
        return recipe.outputFragment().getItems().length == 0
                ? ItemStack.EMPTY
                : recipe.outputFragment().getItems()[0].copyWithCount(recipe.outputCount());
    }

    /** The tier's token rendered at the tier's per-cycle output count. */
    private static ItemStack token(StructureMinerJeiRecipe recipe) {
        return recipe.outputToken().getItems().length == 0
                ? ItemStack.EMPTY
                : recipe.outputToken().getItems()[0].copyWithCount(recipe.outputCount());
    }

    private static boolean isRenderable(@Nullable Fluid fluid) {
        return fluid != null && fluid != Fluids.EMPTY;
    }
}