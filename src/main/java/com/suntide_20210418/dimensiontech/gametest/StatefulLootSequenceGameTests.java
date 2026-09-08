package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.FiniteDistribution;
import com.suntide_20210418.dimensiontech.loot.expectation.LootAnalysisContext;
import com.suntide_20210418.dimensiontech.loot.expectation.PersistentRandomSequenceSnapshot1201;
import com.suntide_20210418.dimensiontech.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.loot.expectation.StatefulLootSequenceExecutor1201;
import com.suntide_20210418.dimensiontech.loot.expectation.XoroshiroState1201;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime continuation checks for ordered root-table execution. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StatefulLootSequenceGameTests {
    private StatefulLootSequenceGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void orderedRootsShareTheRuntimeRandomContinuation(GameTestHelper helper) {
        ResourceLocation first =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/function_order");
        ResourceLocation second =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/function_order");
        LootParams params =
                new LootParams.Builder(helper.getLevel())
                        .withParameter(
                                LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(
                                        helper.absolutePos(BlockPos.ZERO)))
                        .create(LootContextParamSets.CHEST);
        LootContext runtimeContext = new LootContext.Builder(params).create(first);
        XoroshiroState1201 initial =
                PersistentRandomSequenceSnapshot1201.snapshot(helper.getLevel(), first);
        ArrayList<StackState> runtimeOutputs = new ArrayList<>();
        helper.getLevel()
                .getServer()
                .getLootData()
                .getLootTable(first)
                .getRandomItemsRaw(
                        runtimeContext, stack -> runtimeOutputs.add(new StackState(stack)));
        helper.getLevel()
                .getServer()
                .getLootData()
                .getLootTable(second)
                .getRandomItemsRaw(
                        runtimeContext, stack -> runtimeOutputs.add(new StackState(stack)));
        long runtimeNext = runtimeContext.getRandom().nextLong();

        StatefulLootSequenceExecutor1201.Result exact =
                StatefulLootSequenceExecutor1201.execute(
                        helper.getLevel().getServer(),
                        List.of(first, second),
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        initial);
        if (!exact.supported()) {
            helper.fail("Ordered loot sequence failed: " + exact.diagnostics());
        }
        if (!runtimeOutputs.equals(exact.outputs())) {
            helper.fail(
                    "Ordered output mismatch: runtime="
                            + runtimeOutputs
                            + ", exact="
                            + exact.outputs());
        }
        long exactNext = exact.randomState().nextLong().value();
        if (runtimeNext != exactNext) {
            helper.fail(
                    "Ordered random continuation mismatch: runtime="
                            + runtimeNext
                            + ", exact="
                            + exactNext
                            + ", outputs="
                            + exact.outputs());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void singletonStateExpectationRetainsFinalContinuation(GameTestHelper helper) {
        ResourceLocation table =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/function_order");
        XoroshiroState1201 initial =
                PersistentRandomSequenceSnapshot1201.snapshot(helper.getLevel(), table);
        var expectation =
                StatefulLootSequenceExecutor1201.expectation(
                        helper.getLevel().getServer(),
                        List.of(table, table),
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        FiniteDistribution.singleton(initial),
                        32);
        if (expectation.status() != AnalysisStatus.EXACT) {
            helper.fail("Expected exact ordered expectation: " + expectation.diagnostics());
        }
        if (expectation.finalStates().isEmpty()) {
            helper.fail("Exact ordered expectation dropped its final RNG state");
        }
        // The deterministic table call is exercised by the concrete test above; here the
        // continuation PMF is checked against the state returned by the same sequence transition.
        StatefulLootSequenceExecutor1201.Result concrete =
                StatefulLootSequenceExecutor1201.execute(
                        helper.getLevel().getServer(),
                        List.of(table, table),
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        initial);
        if (!expectation.finalStates().get().masses().containsKey(concrete.randomState())) {
            helper.fail("Final continuation PMF does not contain concrete continuation");
        }
        helper.succeed();
    }
}
