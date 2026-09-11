package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipe;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicCrucibleGameTests {
    private MythicCrucibleGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void boundaryInputsConsumeAndResolve(GameTestHelper helper) {
        MythicCrucibleRecipe<ItemStack> recipe =
                MythicCrucibleRecipes.all().stream().findFirst().orElseThrow();

        MythicCrucibleCycle immediate = new MythicCrucibleCycle<ItemStack>();
        immediate.start(recipe);
        MythicCrucibleCycle.Resolution result = immediate.resolve(new ItemStack(Items.QUARTZ));
        helper.assertTrue(result.consumesInput(), "an input must be consumed");
        helper.assertTrue(
                result == MythicCrucibleCycle.Resolution.CORRECT,
                "tick 0 must advance without reward");

        MythicCrucibleCycle rewarded = new MythicCrucibleCycle<ItemStack>();
        rewarded.start(recipe);
        for (int i = 0; i < MythicCrucibleCycle.REWARD_START_TICK; i++) rewarded.tick();
        helper.assertTrue(
                rewarded.resolve(new ItemStack(Items.QUARTZ))
                        == MythicCrucibleCycle.Resolution.CORRECT_REWARDED,
                "tick 20 must reward");

        MythicCrucibleCycle<ItemStack> empty = new MythicCrucibleCycle<>();
        empty.start(recipe);
        empty.tick();
        helper.assertTrue(
                empty.resolve(ItemStack.EMPTY, true) == MythicCrucibleCycle.Resolution.NONE,
                "empty input must not resolve");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void lateAndWrongInputsUsePenalties(GameTestHelper helper) {
        MythicCrucibleRecipe<ItemStack> recipe =
                MythicCrucibleRecipes.all().stream().findFirst().orElseThrow();
        MythicCrucibleCycle late = new MythicCrucibleCycle<ItemStack>();
        late.start(recipe);
        for (int i = 0; i <= MythicCrucibleCycle.STATE_TIMEOUT_TICKS; i++) late.tick();
        MythicCrucibleCycle.Resolution phase = late.resolve(new ItemStack(Items.QUARTZ));
        helper.assertTrue(
                phase == MythicCrucibleCycle.Resolution.PHASE_IDLE, "tick 101 must phase-idle");
        helper.assertTrue(phase.consumesInput(), "late input must be consumed");

        MythicCrucibleCycle wrong = new MythicCrucibleCycle<ItemStack>();
        wrong.start(recipe);
        MythicCrucibleCycle.Resolution penalty = wrong.resolve(new ItemStack(Items.REDSTONE));
        helper.assertTrue(penalty.consumesInput(), "wrong input must be consumed");
        helper.assertTrue(
                penalty == MythicCrucibleCycle.Resolution.STABILIZE_FAILURE,
                "stabilize at branch must use its specific penalty");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 20)
    public static void fifthTierAlternatesOnlyAfterCommit(GameTestHelper helper) {
        MythicCrucibleRecipe<ItemStack> recipe =
                MythicCrucibleRecipes.all().stream()
                        .filter(MythicCrucibleRecipe::hasAlternateBranch)
                        .findFirst()
                        .orElseThrow();
        MythicCrucibleCycle cycle = new MythicCrucibleCycle<ItemStack>();
        cycle.start(recipe);
        resolve(cycle, Items.QUARTZ);
        resolve(cycle, Items.AMETHYST_SHARD);
        resolve(cycle, Items.AMETHYST_SHARD);
        resolve(cycle, Items.GLOWSTONE_DUST);
        resolve(cycle, Items.REDSTONE);
        helper.assertTrue(
                cycle.status() == MythicCrucibleCycle.Status.READY_TO_COMMIT,
                "branch A must reach commit state");
        helper.assertTrue(
                cycle.branch() == MythicCrucibleRecipe.Branch.A,
                "branch must not change before commit");
        cycle.commit();
        helper.assertTrue(
                cycle.branch() == MythicCrucibleRecipe.Branch.B,
                "branch must change once after commit");
        helper.succeed();
    }

    private static void resolve(MythicCrucibleCycle<ItemStack> cycle, Item item) {
        cycle.resolve(new ItemStack(item));
    }
}
