package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.item.ModItems;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * JEI integration for the structure miner and the dimension-deconstruction core.
 *
 * <p>Each miner tier gets its own category and recipe page (clicking a tier-3 miner shows only
 * tier-3), and the deconstruction core gets a standalone source page. JEI discovers this via the
 * {@link JeiPlugin} annotation and a public no-argument constructor, so the class must stay
 * instantiable that way. JEI rebuilds categories and recipes together on a recipe reload, which makes
 * this the right place to read the live tier registry and config.
 */
@JeiPlugin
public final class StructureMinerJeiPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_UID =
            new ResourceLocation(DimensionTechMod.MOD_ID, "structure_miner");

    public static final int TIER_COUNT = 6;

    public static final RecipeType<StructureMinerJeiRecipe>[] TIER_RECIPE_TYPES = tierRecipeTypes();
    public static final RecipeType<DeconstructionCoreJeiRecipe> CORE_TYPE =
            RecipeType.create(
                    DimensionTechMod.MOD_ID, "deconstruction_core", DeconstructionCoreJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            registration.addRecipeCategories(
                    new StructureMinerJeiCategory(
                            registration.getJeiHelpers().getGuiHelper(),
                            tier,
                            TIER_RECIPE_TYPES[tier - 1],
                            minerBlock(tier)));
        }
        registration.addRecipeCategories(
                new DeconstructionCoreJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<StructureMinerJeiRecipe> all =
                StructureMinerJeiRecipes.build(
                        BaseMinerBlockEntity.FLUID_PER_WORK_CYCLE_MB,
                        BaseMinerBlockEntity.FLUID_TANK_CAPACITY_MB,
                        baseParallelByTier(),
                        ModFluids::forMinerTier,
                        tier -> Ingredient.of(ModItems.DIMENSION_FRAGMENTS[tier - 1].get()),
                        tier -> Ingredient.of(ModItems.MINING_TOKENS[tier - 1].get()));
        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            StructureMinerJeiRecipe structure = all.get(tier - 1);
            // Two entries on one page: the structure-marker recipe and its chest-marker variant.
            registration.addRecipes(
                    TIER_RECIPE_TYPES[tier - 1],
                    List.of(structure, structure.asChestVariant()));
        }
        registration.addRecipes(CORE_TYPE, DeconstructionCoreJeiRecipes.build());
    }

    /** Each tier's miner block is its own page's catalyst; the core is its page's catalyst. */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            registration.addRecipeCatalysts(TIER_RECIPE_TYPES[tier - 1], minerBlock(tier));
        }
        registration.addRecipeCatalysts(
                CORE_TYPE, ModItems.DIMENSION_DECONSTRUCTION_CORE.get());
    }

    private static Block minerBlock(int tier) {
        return switch (tier) {
            case 1 -> ModBlocks.TIER_1_STRUCTURE_MINER.get();
            case 2 -> ModBlocks.TIER_2_STRUCTURE_MINER.get();
            case 3 -> ModBlocks.TIER_3_STRUCTURE_MINER.get();
            case 4 -> ModBlocks.TIER_4_STRUCTURE_MINER.get();
            case 5 -> ModBlocks.TIER_5_STRUCTURE_MINER.get();
            default -> ModBlocks.TIER_6_STRUCTURE_MINER.get();
        };
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<StructureMinerJeiRecipe>[] tierRecipeTypes() {
        RecipeType<StructureMinerJeiRecipe>[] types = new RecipeType[TIER_COUNT];
        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            types[tier - 1] =
                    RecipeType.create(
                            DimensionTechMod.MOD_ID,
                            "structure_miner/tier_" + tier,
                            StructureMinerJeiRecipe.class);
        }
        return types;
    }

    private static int[] baseParallelByTier() {
        int[] parallels = new int[ModConfigs.TIERS.length];
        for (int index = 0; index < parallels.length; index++) {
            parallels[index] = ModConfigs.TIERS[index].baseParallel();
        }
        return parallels;
    }
}