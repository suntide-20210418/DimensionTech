package com.suntide_20210418.dimensiontech.gametest;

import com.google.gson.JsonParser;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.DistributionalFunction1201;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.LootAnalysisContext;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StatefulFunction1201;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.XoroshiroState1201;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime-registry checks for the 1.20.1 enchant_randomly serializer/run semantics. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantRandomlySemanticsGameTests {
    private EnchantRandomlySemanticsGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void emptyEnchantmentsDynamicallySelectsApplicableEnchantments(
            GameTestHelper helper) {
        LootAnalysisContext context =
                LootAnalysisContext.at(
                        helper.getLevel(),
                        helper.absolutePos(net.minecraft.core.BlockPos.ZERO),
                        0.0F);
        var function =
                JsonParser.parseString(
                        "[{\"function\":\"minecraft:enchant_randomly\","
                                + "\"enchantments\":[]}] ");
        ItemStack input = new ItemStack(Items.DIAMOND_SWORD);

        var distribution =
                DistributionalFunction1201.applyAll(
                        new StackState(input), function, context, 1_000, "/functions");
        if (!distribution.supported()) {
            helper.fail("Distributional empty enchantments was unsupported: " + distribution);
        }
        boolean hasRandomCalls =
                distribution.distribution().masses().keySet().stream()
                        .anyMatch(outcome -> !outcome.calls().isEmpty());
        if (!hasRandomCalls) {
            helper.fail("Empty enchantments did not retain RandomSource calls");
        }
        boolean allEnchanted =
                distribution.distribution().masses().keySet().stream()
                        .allMatch(outcome -> outcome.value().stack().isEnchanted());
        if (!allEnchanted) {
            helper.fail(
                    "Empty enchantments did not dynamically enchant the sword: "
                            + distribution.distribution().masses());
        }

        StatefulFunction1201.Result stateful =
                StatefulFunction1201.applyAll(
                        input,
                        function,
                        context,
                        XoroshiroState1201.fromSeed(0x1201L),
                        "/functions");
        if (!stateful.supported() || !stateful.stack().isEnchanted()) {
            helper.fail("Stateful empty enchantments did not enchant the sword: " + stateful);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void malformedEnchantRandomlyFieldsReturnStructuredPointers(
            GameTestHelper helper) {
        LootAnalysisContext context =
                LootAnalysisContext.at(
                        helper.getLevel(),
                        helper.absolutePos(net.minecraft.core.BlockPos.ZERO),
                        0.0F);
        ItemStack input = new ItemStack(Items.STONE);

        var malformedCandidates =
                JsonParser.parseString(
                        "[{\"function\":\"minecraft:enchant_randomly\"," + "\"enchantments\":{}}]");
        var distributional =
                DistributionalFunction1201.applyAll(
                        new StackState(input), malformedCandidates, context, 100, "/functions");
        if (distributional.supported()
                || !"/functions/0/enchantments".equals(distributional.pointer())) {
            helper.fail("Malformed enchantments lost its pointer: " + distributional);
        }
        var stateful =
                StatefulFunction1201.applyAll(
                        input,
                        malformedCandidates,
                        context,
                        XoroshiroState1201.fromSeed(7L),
                        "/functions");
        if (stateful.supported() || !"/functions/0/enchantments".equals(stateful.pointer())) {
            helper.fail("Stateful malformed enchantments lost its pointer: " + stateful);
        }

        var malformedTreasure =
                JsonParser.parseString(
                        "[{\"function\":\"minecraft:enchant_with_levels\","
                                + "\"levels\":1,\"treasure\":{}}]");
        var malformedDistributional =
                DistributionalFunction1201.applyAll(
                        new StackState(new ItemStack(Items.DIAMOND_SWORD)),
                        malformedTreasure,
                        context,
                        100,
                        "/functions");
        if (malformedDistributional.supported()
                || !"/functions/0/treasure".equals(malformedDistributional.pointer())) {
            helper.fail("Malformed treasure lost its pointer: " + malformedDistributional);
        }
        var malformedStateful =
                StatefulFunction1201.applyAll(
                        new ItemStack(Items.DIAMOND_SWORD),
                        malformedTreasure,
                        context,
                        XoroshiroState1201.fromSeed(9L),
                        "/functions");
        if (malformedStateful.supported()
                || !"/functions/0/treasure".equals(malformedStateful.pointer())) {
            helper.fail("Stateful malformed treasure lost its pointer: " + malformedStateful);
        }

        var malformedType = JsonParser.parseString("[{\"function\":{}}]");
        var typeResult =
                DistributionalFunction1201.applyAll(
                        new StackState(input), malformedType, context, 100, "/functions");
        if (typeResult.supported() || !"/functions/0/function".equals(typeResult.pointer())) {
            helper.fail("Malformed function type escaped as an exception: " + typeResult);
        }
        helper.succeed();
    }
}
