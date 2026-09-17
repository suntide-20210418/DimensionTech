package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipes;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The crucible's JEI page: input fluid, fragment, output fluid, and the ritual state sequence with
 * the one operation item each state demands.
 *
 * <p>JEI fixes a category's width and height per category rather than per recipe, so the row count
 * is derived once from the registry's longest sequence (see {@link
 * MythicCrucibleJeiRecipes#visibleRows(int)}) and anything longer is summarised instead of drawn.
 */
public final class MythicCrucibleJeiCategory
        extends AbstractRecipeCategory<MythicCrucibleJeiRecipe> {
    private static final int WIDTH = 184;

    private static final int TOP_Y = 2;
    private static final int FLUID_SIZE = 16;
    private static final int INPUT_X = 2;
    private static final int FRAGMENT_X = 24;
    private static final int ARROW_X = 50;
    private static final int OUTPUT_X = 78;

    private static final int TEXT_X = 2;
    private static final int HEADER_Y0 = 22;
    private static final int HEADER_Y1 = 32;
    private static final int HEADER_Y2 = 42;
    private static final int SEQUENCE_TITLE_Y = 56;

    private static final int ROW_START_Y = 66;
    private static final int ROW_H = 18;
    private static final int ROW_TEXT_INSET = 4;
    private static final int STEP_X = 2;
    private static final int STEP_TEXT_X = 22;

    private static final int FOOTER_GAP = 5;
    private static final int FOOTER_LINE_H = 10;
    private static final int FOOTER_LINES = 5;

    /** Minecraft's default font line height; text widgets add their line spacing on top of it. */
    private static final int FONT_LINE_H = 9;

    /** The wrapping area height of a single line of text. */
    private static final int LINE_H = 10;

    private static final String SLOT_INPUT = "input";
    private static final String SLOT_FRAGMENT = "fragment";
    private static final String SLOT_OUTPUT = "output";
    private static final String SLOT_STEP = "step";

    private static final int RULE = 0xFF3D475B;

    private final int rows;

    /**
     * The row count is fixed here, once per category registration, because JEI sizes a category
     * rather than an individual recipe.
     */
    public MythicCrucibleJeiCategory(IGuiHelper guiHelper) {
        this(
                guiHelper,
                MythicCrucibleJeiRecipes.visibleRows(
                        MythicCrucibleJeiRecipes.longestSequence(MythicCrucibleRecipes.all())));
    }

    private MythicCrucibleJeiCategory(IGuiHelper guiHelper, int rows) {
        super(
                MythicCrucibleJeiPlugin.RECIPE_TYPE,
                Component.translatable(MythicCrucibleJeiText.TITLE),
                guiHelper.createDrawableItemLike(ModBlocks.MYTHIC_CRUCIBLE.get()),
                WIDTH,
                heightFor(rows));
        this.rows = rows;
    }

    static int heightFor(int rows) {
        return ROW_START_Y + rows * ROW_H + FOOTER_GAP + FOOTER_LINES * FOOTER_LINE_H + FOOTER_GAP;
    }

    /** The recipe's own registry name plus its branch, so A and B bookmark separately. */
    @Override
    public ResourceLocation getRegistryName(MythicCrucibleJeiRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(
                recipe.id().getNamespace(),
                recipe.id().getPath() + "/" + recipe.branch().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder, MythicCrucibleJeiRecipe recipe, IFocusGroup focuses) {
        if (isRenderable(recipe.input())) {
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, TOP_Y)
                    .addFluidStack(recipe.input(), recipe.inputAmountMb())
                    .setFluidRenderer(recipe.inputAmountMb(), false, FLUID_SIZE, FLUID_SIZE)
                    .setSlotName(SLOT_INPUT);
        }
        if (recipe.fragment() != null && !recipe.fragment().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, FRAGMENT_X, TOP_Y + 1)
                    .addItemStacks(fragmentStacks(recipe))
                    .setStandardSlotBackground()
                    .setSlotName(SLOT_FRAGMENT);
        }
        if (isRenderable(recipe.output())) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, TOP_Y)
                    .addFluidStack(recipe.output(), recipe.outputAmountMb())
                    .setFluidRenderer(recipe.outputAmountMb(), false, FLUID_SIZE, FLUID_SIZE)
                    .setSlotName(SLOT_OUTPUT);
        }
        for (int index = 0; index < drawableSteps(recipe); index++) {
            addStepSlot(builder, recipe, index);
        }
    }

    private void addStepSlot(
            IRecipeLayoutBuilder builder, MythicCrucibleJeiRecipe recipe, int index) {
        MythicCrucibleJeiRecipe.DisplayStep step = recipe.steps().get(index);
        int number = index + 1;
        IRecipeSlotBuilder slot =
                builder.addSlot(RecipeIngredientRole.INPUT, STEP_X, ROW_START_Y + index * ROW_H)
                        .setStandardSlotBackground()
                        .setSlotName(SLOT_STEP + index)
                        .addRichTooltipCallback(
                                (view, tooltip) ->
                                        MythicCrucibleJeiText.appendStepTooltip(
                                                tooltip, step, number));
        if (step.operation() != null && !step.operation().isEmpty()) {
            slot.addIngredients(step.operation());
        }
    }

    @Override
    public void createRecipeExtras(
            IRecipeExtrasBuilder builder, MythicCrucibleJeiRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(ARROW_X, TOP_Y + 1);
        line(builder, MythicCrucibleJeiText.ioLine(recipe), TEXT_X, HEADER_Y0);
        line(builder, MythicCrucibleJeiText.branchLine(recipe), TEXT_X, HEADER_Y1);
        if (recipe.alternateBranches()) {
            line(builder, MythicCrucibleJeiText.alternateLine(), TEXT_X, HEADER_Y2);
        }
        line(
                builder,
                Component.translatable(MythicCrucibleJeiText.SEQUENCE),
                TEXT_X,
                SEQUENCE_TITLE_Y);
        int drawable = drawableSteps(recipe);
        for (int index = 0; index < drawable; index++) {
            line(
                    builder,
                    MythicCrucibleJeiText.rowLine(recipe.steps().get(index)),
                    STEP_TEXT_X,
                    ROW_START_Y + index * ROW_H + ROW_TEXT_INSET);
        }
        if (recipe.steps().size() > drawable) {
            line(
                    builder,
                    MythicCrucibleJeiText.moreLine(recipe.steps().size() - drawable),
                    STEP_TEXT_X,
                    ROW_START_Y + drawable * ROW_H + ROW_TEXT_INSET);
        }
        builder.addText(
                        MythicCrucibleJeiText.footerLines(recipe),
                        WIDTH - TEXT_X * 2,
                        FOOTER_LINES * FOOTER_LINE_H)
                .setPosition(TEXT_X, footerY())
                .setLineSpacing(FOOTER_LINE_H - FONT_LINE_H);
    }

    /**
     * One positioned line of text. {@code addText} takes the wrapping area, not a position: the
     * widget has to be told where to sit afterwards, or every line piles up in the top-left corner.
     */
    private static void line(IRecipeExtrasBuilder builder, FormattedText text, int x, int y) {
        builder.addText(text, WIDTH - x - TEXT_X, LINE_H).setPosition(x, y);
    }

    @Override
    public void draw(
            MythicCrucibleJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY) {
        rule(graphics, SEQUENCE_TITLE_Y - 4);
        rule(graphics, footerY() - 4);
    }

    private void rule(GuiGraphics graphics, int y) {
        graphics.fill(TEXT_X, y, WIDTH - TEXT_X, y + 1, RULE);
    }

    /**
     * Hovering a row's text repeats the detail of its operation slot. The slot keeps its own rich
     * tooltip, so the slot's own area is deliberately skipped here to avoid printing it twice.
     */
    @Override
    public void getTooltip(
            ITooltipBuilder tooltip,
            MythicCrucibleJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        if (mouseX < STEP_TEXT_X - 2) return;
        int index = rowAt(mouseY);
        if (index < 0 || index >= drawableSteps(recipe)) return;
        MythicCrucibleJeiText.appendStepTooltip(tooltip, recipe.steps().get(index), index + 1);
    }

    private int rowAt(double mouseY) {
        if (mouseY < ROW_START_Y || mouseY >= ROW_START_Y + rows * ROW_H) return -1;
        return (int) ((mouseY - ROW_START_Y) / ROW_H);
    }

    private int drawableSteps(MythicCrucibleJeiRecipe recipe) {
        return Math.min(recipe.steps().size(), rows);
    }

    private int footerY() {
        return ROW_START_Y + rows * ROW_H + FOOTER_GAP;
    }

    /** The recipe's fragment ingredient rendered at the recipe's own fragment count. */
    private static List<ItemStack> fragmentStacks(MythicCrucibleJeiRecipe recipe) {
        Ingredient fragment = recipe.fragment();
        if (fragment == null) return List.of();
        return java.util.Arrays.stream(fragment.getItems())
                .map(stack -> stack.copyWithCount(recipe.fragmentCount()))
                .toList();
    }

    private static boolean isRenderable(Fluid fluid) {
        return fluid != null && fluid != Fluids.EMPTY;
    }
}
