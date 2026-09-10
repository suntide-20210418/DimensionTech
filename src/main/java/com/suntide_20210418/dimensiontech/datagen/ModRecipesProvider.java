package com.suntide_20210418.dimensiontech.datagen;

import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

public class ModRecipesProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipesProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> writer) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.AMETHYST_SHARD, 4)
                .requires(Items.AMETHYST_BLOCK)
                .unlockedBy("has_amethyst_block", has(Items.AMETHYST_BLOCK))
                .save(writer);
    }
}
