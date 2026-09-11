package com.suntide_20210418.dimensiontech.mythiccrucible;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.fluid.ModFluids;
import com.suntide_20210418.dimensiontech.item.ModItems;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;

/**
 * In-memory recipe registry. Defaults are restored on datapack/KubeJS reload before scripts run.
 */
public final class MythicCrucibleRecipes {
    private static final Map<
                    ResourceLocation, MythicCrucibleRecipe<net.minecraft.world.item.ItemStack>>
            RECIPES = new LinkedHashMap<>();

    private MythicCrucibleRecipes() {}

    public static synchronized void resetDefaults() {
        RECIPES.clear();
        add(
                defaultRecipe(
                        "initial_manifestation",
                        Fluids.WATER,
                        ModFluids.MYTHIC_ESSENCE.source().get(),
                        1,
                        "branch converge stabilize"));
        add(
                defaultRecipe(
                        "mythic_surge",
                        ModFluids.MYTHIC_ESSENCE.source().get(),
                        ModFluids.SURGING_MYTHIC_ESSENCE.source().get(),
                        2,
                        "branch recurse converge stabilize"));
        add(
                defaultRecipe(
                        "recursive_foundation",
                        ModFluids.SURGING_MYTHIC_ESSENCE.source().get(),
                        ModFluids.RECURSIVE_ESSENCE.source().get(),
                        3,
                        "branch recurse recurse converge stabilize"));
        add(
                defaultRecipe(
                        "recursive_surge",
                        ModFluids.RECURSIVE_ESSENCE.source().get(),
                        ModFluids.SURGING_RECURSIVE_ESSENCE.source().get(),
                        4,
                        "branch recurse recurse recurse converge stabilize"));
        add(
                new MythicCrucibleRecipe(
                        DimensionTechMod.MOD_ID.equals("dimension_tech")
                                ? ResourceLocation.fromNamespaceAndPath(
                                        DimensionTechMod.MOD_ID, "fractal_closure")
                                : ResourceLocation.parse("dimension_tech:fractal_closure"),
                        ModFluids.SURGING_RECURSIVE_ESSENCE.source().get(),
                        ModFluids.FRACTAL_ESSENCE.source().get(),
                        Ingredient.of(ModItems.DIMENSION_FRAGMENTS[4].get()),
                        1,
                        1_000,
                        1_000,
                        sequence("branch recurse recurse converge stabilize"),
                        sequence("branch recurse branch recurse converge stabilize")));
    }

    private static MythicCrucibleRecipe<net.minecraft.world.item.ItemStack> defaultRecipe(
            String path,
            net.minecraft.world.level.material.Fluid input,
            net.minecraft.world.level.material.Fluid output,
            int fragmentTier,
            String dsl) {
        return new MythicCrucibleRecipe(
                ResourceLocation.fromNamespaceAndPath(DimensionTechMod.MOD_ID, path),
                input,
                output,
                Ingredient.of(ModItems.DIMENSION_FRAGMENTS[fragmentTier - 1].get()),
                1,
                1_000,
                1_000,
                sequence(dsl),
                null);
    }

    public static synchronized void add(
            MythicCrucibleRecipe<net.minecraft.world.item.ItemStack> recipe) {
        RECIPES.put(recipe.id(), recipe);
    }

    public static synchronized MythicCrucibleRecipe<net.minecraft.world.item.ItemStack> get(
            ResourceLocation id) {
        return RECIPES.get(id);
    }

    public static synchronized Collection<MythicCrucibleRecipe<net.minecraft.world.item.ItemStack>>
            all() {
        return List.copyOf(RECIPES.values());
    }

    public static synchronized MythicCrucibleRecipe<net.minecraft.world.item.ItemStack>
            firstMatching(net.minecraft.world.level.material.Fluid fluid) {
        return RECIPES.values().stream()
                .filter(recipe -> recipe.input() == fluid)
                .findFirst()
                .orElse(null);
    }

    public static List<StateStep<net.minecraft.world.item.ItemStack>> sequence(String dsl) {
        return List.of(dsl.trim().split("\\s+|→|->")).stream()
                .filter(value -> !value.isBlank())
                .map(MythicCrucibleRecipes::step)
                .toList();
    }

    private static StateStep<net.minecraft.world.item.ItemStack> step(String name) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "branch" ->
                    new StateStep(StateId.BRANCH, OperationMatcher.of(Ingredient.of(Items.QUARTZ)));
            case "recurse" ->
                    new StateStep(
                            StateId.RECURSE,
                            OperationMatcher.of(Ingredient.of(Items.AMETHYST_SHARD)));
            case "converge" ->
                    new StateStep(
                            StateId.CONVERGE,
                            OperationMatcher.of(Ingredient.of(Items.GLOWSTONE_DUST)));
            case "stabilize" ->
                    new StateStep(
                            StateId.STABILIZE, OperationMatcher.of(Ingredient.of(Items.REDSTONE)));
            default -> throw new IllegalArgumentException("Unknown crucible state: " + name);
        };
    }
}
