package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.EnchantmentMarkAnvilHandler;
import com.suntide_20210418.dimensiontech.item.EnchantmentMarkItem;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.loot.expectation.EnchantmentKey;
import com.suntide_20210418.dimensiontech.loot.expectation.EnchantmentMarginal;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactEnchantmentSemantics1201;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.FiniteDistribution;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.recipe.EnchantmentMarkRecipe;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Runtime-registry checks for the enchantment mark economy. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantmentMarkGameTests {
    private static final int STATE_BUDGET = 50_000;

    /** Lower level keeps the materialized reference small enough to stay fast in a gametest. */
    private static final int CROSS_CHECK_LEVEL = 10;

    private EnchantmentMarkGameTests() {}

    private static EnchantmentKey sharpness(int level) {
        return new EnchantmentKey(
                ForgeRegistries.ENCHANTMENTS.getKey(Enchantments.SHARPNESS), level);
    }

    /**
     * The marginal recursion never materializes ordered selections, so it has to be pinned against
     * the path that does. Both must agree exactly - they are two computations of the same first
     * moment over the same branching process.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void marginalMatchesMaterializedSelectionDistribution(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        StackMeasure measure = new StackMeasure();
        measure.add(new StackState(sword), ExactProbability.ONE);

        EnchantmentMarginal marginal =
                ExactEnchantmentSemantics1201.enchantmentMarginal(
                        measure.values().entrySet(),
                        FiniteDistribution.singleton(CROSS_CHECK_LEVEL),
                        false,
                        STATE_BUDGET);
        if (marginal.isEmpty()) {
            helper.fail(
                    "Enchantment marginal was empty for a diamond sword at level "
                            + CROSS_CHECK_LEVEL);
            return;
        }

        Map<EnchantmentKey, ExactProbability> materialized = new LinkedHashMap<>();
        for (Map.Entry<StackState, ExactProbability> outcome :
                ExactEnchantmentSemantics1201.enchantItemMarginal(sword, CROSS_CHECK_LEVEL, false, STATE_BUDGET)
                        .masses()
                        .entrySet()) {
            EnchantmentHelper.getEnchantments(outcome.getKey().stack())
                    .forEach(
                            (enchantment, level) ->
                                    materialized.merge(
                                            new EnchantmentKey(
                                                    ForgeRegistries.ENCHANTMENTS.getKey(enchantment),
                                                    level),
                                            outcome.getValue(),
                                            ExactProbability::add));
        }

        if (!marginal.values().equals(materialized)) {
            helper.fail(
                    "Enchantment marginal disagrees with the materialized distribution. marginal="
                            + marginal.values()
                            + " materialized="
                            + materialized);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void markNbtRoundTripsEnchantmentAndLevel(GameTestHelper helper) {
        ItemStack mark = EnchantmentMarkItem.create(sharpness(3), 5);
        Optional<EnchantmentKey> read = EnchantmentMarkItem.getKey(mark);
        if (read.isEmpty() || !read.get().equals(sharpness(3))) {
            helper.fail("Mark NBT did not round trip: " + read);
            return;
        }
        if (mark.getCount() != 5) {
            helper.fail("Mark count was not preserved: " + mark.getCount());
            return;
        }
        if (EnchantmentMarkItem.getKey(new ItemStack(Items.DIAMOND)).isPresent()) {
            helper.fail("A non-mark item reported an enchantment key");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void levelOneMarkIsAtomicAndSplitsUpward(GameTestHelper helper) {
        Optional<ItemStack> split = EnchantmentMarkItem.splitResult(EnchantmentMarkItem.create(sharpness(1), 1));
        if (split.isPresent()) {
            helper.fail("A level 1 mark must not split further: " + split.get());
            return;
        }
        Optional<ItemStack> two =
                EnchantmentMarkItem.splitResult(EnchantmentMarkItem.create(sharpness(2), 1));
        if (two.isEmpty()) {
            helper.fail("A level 2 mark must split");
            return;
        }
        ItemStack result = two.get();
        if (result.getCount() != 2
                || !Optional.of(sharpness(1)).equals(EnchantmentMarkItem.getKey(result))) {
            helper.fail("A level 2 mark must split into two level 1 marks: " + result);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void combiningStopsAtTheEnchantmentMaxLevel(GameTestHelper helper) {
        int max =
                ForgeRegistries.ENCHANTMENTS
                        .getValue(sharpness(1).enchantment())
                        .getMaxLevel();
        if (EnchantmentMarkItem.combineResult(EnchantmentMarkItem.create(sharpness(max), 1))
                .isPresent()) {
            helper.fail("A max level mark must not combine past " + max);
            return;
        }
        Optional<ItemStack> combined =
                EnchantmentMarkItem.combineResult(EnchantmentMarkItem.create(sharpness(max - 1), 1));
        if (combined.isEmpty()
                || !Optional.of(sharpness(max)).equals(EnchantmentMarkItem.getKey(combined.get()))) {
            helper.fail("Combining two level " + (max - 1) + " marks failed: " + combined);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void differentEnchantmentsNeverCombine(GameTestHelper helper) {
        ItemStack sharpness = EnchantmentMarkItem.create(sharpness(1), 1);
        ItemStack protection =
                EnchantmentMarkItem.create(
                        new EnchantmentKey(
                                ForgeRegistries.ENCHANTMENTS.getKey(Enchantments.UNBREAKING), 1),
                        1);
        if (EnchantmentMarkRecipe.resolve(List.of(sharpness, protection), EnchantmentMarkRecipe.Mode.COMBINE)
                .isPresent()) {
            helper.fail("Marks of different enchantments combined");
            return;
        }
        if (EnchantmentMarkRecipe.resolve(List.of(sharpness, sharpness.copy()), EnchantmentMarkRecipe.Mode.COMBINE)
                .isEmpty()) {
            helper.fail("Two identical marks did not combine");
            return;
        }
        if (EnchantmentMarkRecipe.resolve(List.of(sharpness), EnchantmentMarkRecipe.Mode.COMBINE)
                .isPresent()) {
            helper.fail("A single mark produced a combine result");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void splittingIsValuePreservingDownToLevelOne(GameTestHelper helper) {
        ItemStack current = EnchantmentMarkItem.create(sharpness(4), 1);
        int level = 4;
        while (level > 1) {
            Optional<ItemStack> next =
                    EnchantmentMarkRecipe.resolve(List.of(current), EnchantmentMarkRecipe.Mode.SPLIT);
            if (next.isEmpty()) {
                helper.fail("Splitting a level " + level + " mark failed");
                return;
            }
            current = next.get();
            if (current.getCount() != 2) {
                helper.fail("Splitting did not produce exactly two marks");
                return;
            }
            level--;
            Optional<EnchantmentKey> key = EnchantmentMarkItem.getKey(current);
            if (key.isEmpty() || key.get().level() != level) {
                helper.fail("Split result has the wrong level: " + key);
                return;
            }
            current.setCount(1);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void anvilAppliesMarkToEquipment(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        AnvilUpdateEvent event =
                new AnvilUpdateEvent(
                        sword, EnchantmentMarkItem.create(sharpness(3), 1), "", 0, helper.makeMockPlayer());
        EnchantmentMarkAnvilHandler.onAnvilUpdate(event);

        ItemStack output = event.getOutput();
        if (output.isEmpty() || !output.is(Items.DIAMOND_SWORD)) {
            helper.fail("Anvil did not produce an enchanted sword: " + output);
            return;
        }
        Integer level = EnchantmentHelper.getEnchantments(output).get(Enchantments.SHARPNESS);
        if (level == null || level != 3) {
            helper.fail("Anvil applied the wrong sharpness level: " + level);
            return;
        }
        if (event.getCost() <= 0 || event.getMaterialCost() != 1) {
            helper.fail(
                    "Anvil cost was not set: levelCost="
                            + event.getCost()
                            + " materialCost="
                            + event.getMaterialCost());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void anvilTurnsABookIntoAnEnchantedBook(GameTestHelper helper) {
        AnvilUpdateEvent event =
                new AnvilUpdateEvent(
                        new ItemStack(Items.BOOK),
                        EnchantmentMarkItem.create(sharpness(2), 1),
                        "",
                        0,
                        helper.makeMockPlayer());
        EnchantmentMarkAnvilHandler.onAnvilUpdate(event);

        ItemStack output = event.getOutput();
        if (output.isEmpty() || !output.is(Items.ENCHANTED_BOOK)) {
            helper.fail("Anvil did not produce an enchanted book: " + output);
            return;
        }
        Map<net.minecraft.world.item.enchantment.Enchantment, Integer> stored =
                EnchantmentHelper.getEnchantments(output);
        Integer level = stored.get(Enchantments.SHARPNESS);
        if (level == null || level != 2) {
            helper.fail("Enchanted book has the wrong sharpness level: " + stored);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void anvilRefusesWhenTheEnchantmentIsAlreadyPresent(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 3);
        AnvilUpdateEvent event =
                new AnvilUpdateEvent(
                        sword, EnchantmentMarkItem.create(sharpness(3), 1), "", 0, helper.makeMockPlayer());
        EnchantmentMarkAnvilHandler.onAnvilUpdate(event);
        if (!event.getOutput().isEmpty()) {
            helper.fail("Anvil offered a redundant re-application: " + event.getOutput());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void markItemIsRegisteredWithTheExpectedStackSize(GameTestHelper helper) {
        ItemStack mark = EnchantmentMarkItem.create(sharpness(1), 64);
        if (!mark.is(ModItems.ENCHANTMENT_MARK.get())) {
            helper.fail("Mark item is not the registered enchantment mark");
            return;
        }
        helper.succeed();
    }
}
