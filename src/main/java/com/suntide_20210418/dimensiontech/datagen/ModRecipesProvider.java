package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.item.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Recipe data for the whole mod.
 *
 * <p>Two rules drive every entry here. First, a tier's fragments and mining tokens only ever come
 * out of that tier's miner, so nothing needed to build a Tier N machine may consume Tier N
 * fragments or tokens. The focus blocks therefore chain upward through the previous tier, the miner
 * controllers consume the previous tier's fragment and token, and only the upgrade blocks spend
 * their own tier's products. Second, the mod's real gates are energy, structure exploration and the
 * reactor fluid chain, so material costs stay on cheap vanilla items on purpose.
 */
public class ModRecipesProvider extends RecipeProvider implements IConditionBuilder {
    private static final String[] SPECIALIZATIONS = {"efficiency", "parallel", "luck", "energy"};

    public ModRecipesProvider(
            PackOutput pOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(pOutput, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput writer) {
        amethystDecomposition(writer);
        entryTools(writer);
        frameParts(writer);
        machines(writer);
        upgradeBlocks(writer);
    }

    /** Tools that depend on no machine product, so the loop can be entered at all. */
    private void entryTools(RecipeOutput writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STRUCTURE_MARKER.get())
                .pattern("IQI")
                .pattern("AGA")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('Q', Items.QUARTZ)
                .define('A', Items.AMETHYST_SHARD)
                .define('G', Items.GLASS_PANE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHEST_MARKER.get())
                .pattern("IAI")
                .pattern("QGR")
                .pattern("IAI")
                .define('I', Items.IRON_INGOT)
                .define('Q', Items.QUARTZ)
                .define('A', Items.AMETHYST_SHARD)
                .define('G', Items.GLASS_PANE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(writer);

        // Reads the structures the player has already explored, so it sits at the tier two stage.
        // Fragments and tokens are the gate; the vanilla parts stay minimal so the grid fits.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_INTEGRATOR.get())
                .pattern("FQF")
                .pattern("ACA")
                .pattern("TRT")
                .define('F', ModItems.DIMENSION_FRAGMENTS[1].get())
                .define('Q', Items.QUARTZ)
                .define('A', Items.AMETHYST_SHARD)
                .define('C', ModItems.STRUCTURE_MINER_CASING.get())
                .define('T', ModItems.MINING_TOKENS[1].get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_mining_token_tier_2", has(ModItems.MINING_TOKENS[1].get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WRENCH.get())
                .pattern(" I ")
                .pattern(" SI")
                .pattern("S  ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        // Reads every structure in the game, which skips exploration outright, so it is late.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STRUCTURE_INTERPRETER.get())
                .pattern("FNF")
                .pattern("SCS")
                .pattern("TNT")
                .define('F', ModItems.DIMENSION_FRAGMENTS[4].get())
                .define('N', Items.NETHERITE_INGOT)
                .define('S', Items.NETHER_STAR)
                .define('C', ModItems.DATA_INTEGRATOR.get())
                .define('T', ModItems.MINING_TOKENS[4].get())
                .unlockedBy("has_mining_token_tier_5", has(ModItems.MINING_TOKENS[4].get()))
                .save(writer);
    }

    /**
     * Vanilla only lets amethyst shards be packed into a block, never unpacked, which makes bulk
     * amethyst awkward in a mod that spends it on every tier. The conversion is deliberately lossy
     * (four shards back from nine) so it cannot be looped for free.
     */
    private void amethystDecomposition(RecipeOutput writer) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.AMETHYST_SHARD, 4)
                .requires(Items.AMETHYST_BLOCK)
                .unlockedBy("has_amethyst_block", has(Items.AMETHYST_BLOCK))
                .save(writer);
    }

    /**
     * Mass-produced frame parts; one machine eats 44 casings, so the yield is deliberately high.
     */
    private void frameParts(RecipeOutput writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STRUCTURE_MINER_CASING.get())
                .pattern("INI")
                .pattern("RDR")
                .pattern("INI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('N', Items.QUARTZ)
                .define('D', Items.DIAMOND)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STRUCTURE_MINER_STRUCTURE.get())
                .pattern("QAQ")
                .pattern("AIA")
                .pattern("RAR")
                .define('Q', Items.QUARTZ)
                .define('A', Items.AMETHYST_SHARD)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STRUCTURE_MINER_GLASS.get(), 8)
                .pattern("GGG")
                .pattern("GAG")
                .pattern("GGG")
                .define('G', Items.GLASS)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(writer);
    }

    private void machines(RecipeOutput writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STRUCTURE_DATA_OPERATOR.get())
                .pattern("SGS")
                .pattern("AIA")
                .pattern("ORO")
                .define('S', ModItems.STRUCTURE_MINER_CASING.get())
                .define('G', Items.GLASS)
                .define('A', Items.AMETHYST_SHARD)
                .define('I', Items.IRON_INGOT)
                .define('O', Items.GOLD_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy(
                        "has_structure_miner_casing", has(ModItems.STRUCTURE_MINER_CASING.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STRUCTURE_REACTOR.get())
                .pattern("OSO")
                .pattern("ADA")
                .pattern("OSO")
                .define('O', Items.OBSIDIAN)
                .define('S', ModItems.STRUCTURE_MINER_CASING.get())
                .define('A', Items.AMETHYST_SHARD)
                .define('D', Items.GLOWSTONE_DUST)
                .unlockedBy("has_obsidian", has(Items.OBSIDIAN))
                .save(writer);

        // The first miner is the exploration gate: until a miner exists the core only comes from
        // chest loot, which hands one over within twenty chests at the latest.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TIER_1_STRUCTURE_MINER.get())
                .pattern("IDI")
                .pattern("ECE")
                .pattern("SDS")
                .define('I', Items.IRON_INGOT)
                .define('E', Items.ENDER_PEARL)
                .define('C', ModItems.DIMENSION_DECONSTRUCTION_CORE.get())
                .define('S', ModItems.STRUCTURE_MINER_CASING.get())
                .define('D', Items.DIAMOND)
                .unlockedBy(
                        "has_dimension_deconstruction_core",
                        has(ModItems.DIMENSION_DECONSTRUCTION_CORE.get()))
                .save(writer);

        for (int tier = 2; tier <= 6; tier++) {
            ItemLike previousFragment = ModItems.DIMENSION_FRAGMENTS[tier - 2].get();
            ItemLike previousToken = ModItems.MINING_TOKENS[tier - 2].get();
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, minerItem(tier).get())
                    .pattern("SFS")
                    .pattern("TMT")
                    .pattern("SFS")
                    .define('S', ModItems.STRUCTURE_MINER_CASING.get())
                    .define('F', previousFragment)
                    .define('M', minerItem(tier - 1).get())
                    .define('T', previousToken)
                    .unlockedBy("has_mining_token_tier_" + (tier - 1), has(previousToken))
                    .save(writer);
        }
    }

    /**
     * Every upgrade block costs the fragments and tokens of its own tier. That is safe because the
     * twelve upgrade slots also accept plain casings, which are pure vanilla, so a first machine
     * never depends on a machine product.
     */
    private void upgradeBlocks(RecipeOutput writer) {
        for (String type : SPECIALIZATIONS) {
            for (int tier = 1; tier <= 6; tier++) {
                ItemLike token = ModItems.MINING_TOKENS[tier - 1].get();
                ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, specializedUpgrade(type, tier).get())
                        .pattern("CTC")
                        .pattern("FSF")
                        .pattern("CTC")
                        .define('S', ModItems.STRUCTURE_MINER_CASING.get())
                        .define('C', signatureMaterial(type))
                        .define('F', ModItems.DIMENSION_FRAGMENTS[tier - 1].get())
                        .define('T', token)
                        .unlockedBy("has_mining_token_tier_" + tier, has(token))
                        .save(writer);
            }
        }

        for (int tier = 1; tier <= 6; tier++) {
            ItemLike fragment = ModItems.DIMENSION_FRAGMENTS[tier - 1].get();
            ItemLike token = ModItems.MINING_TOKENS[tier - 1].get();
            // P parallel, L luck, E efficiency, D energy: one of each specialisation turns into two
            // aggregates, which is a real trade against simply slotting four specialisations.
            ShapedRecipeBuilder.shaped(
                            RecipeCategory.MISC, ModItems.UPGRADE_AGGREGATE_TIERS[tier - 1].get())
                    .pattern("PTE")
                    .pattern("FCF")
                    .pattern("LTD")
                    .define('P', specializedUpgrade("parallel", tier).get())
                    .define('L', specializedUpgrade("luck", tier).get())
                    .define('E', specializedUpgrade("efficiency", tier).get())
                    .define('D', specializedUpgrade("energy", tier).get())
                    .define('F', fragment)
                    .define('C', ModItems.DIMENSION_DECONSTRUCTION_CORE.get())
                    .define('T', token)
                    .unlockedBy("has_mining_token_tier_" + tier, has(token))
                    .save(writer);
        }
    }

    /**
     * The four specialisations reuse the reactor's four operation items, so laying out these
     * recipes teaches the reactor ritual without any extra text.
     */
    private static ItemLike signatureMaterial(String type) {
        return switch (type) {
            case "efficiency" -> Items.QUARTZ; // branch
            case "parallel" -> Items.AMETHYST_SHARD; // recurse
            case "luck" -> Items.GLOWSTONE_DUST; // converge
            case "energy" -> Items.REDSTONE; // stabilize
            default -> throw new IllegalArgumentException("Unknown upgrade type: " + type);
        };
    }

    private static DeferredHolder<Item, Item> specializedUpgrade(String type, int tier) {
        DeferredHolder<Item, Item>[] tiers =
                switch (type) {
                    case "efficiency" -> ModItems.UPGRADE_EFFICIENCY_TIERS;
                    case "parallel" -> ModItems.UPGRADE_PARALLEL_TIERS;
                    case "luck" -> ModItems.UPGRADE_LUCK_TIERS;
                    case "energy" -> ModItems.UPGRADE_ENERGY_TIERS;
                    default -> throw new IllegalArgumentException("Unknown upgrade type: " + type);
                };
        return tiers[tier - 1];
    }

    private static DeferredHolder<Item, Item> minerItem(int tier) {
        return switch (tier) {
            case 1 -> ModItems.TIER_1_STRUCTURE_MINER;
            case 2 -> ModItems.TIER_2_STRUCTURE_MINER;
            case 3 -> ModItems.TIER_3_STRUCTURE_MINER;
            case 4 -> ModItems.TIER_4_STRUCTURE_MINER;
            case 5 -> ModItems.TIER_5_STRUCTURE_MINER;
            case 6 -> ModItems.TIER_6_STRUCTURE_MINER;
            default -> throw new IllegalArgumentException("Unknown miner tier: " + tier);
        };
    }
}
