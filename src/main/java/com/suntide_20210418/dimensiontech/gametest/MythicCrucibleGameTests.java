package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipe;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicCrucibleGameTests {
    private MythicCrucibleGameTests() {}

    /**
     * The tick loop asks the state machine once per tick, and the operation slot is empty on almost
     * all of them. An idle crucible must therefore keep its wait timer running instead of being
     * penalized into a reset every tick.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void anIdleTickLoopKeepsTheWaitTimerRunning(GameTestHelper helper) {
        helper.setBlock(0, 1, 0, ModBlocks.MYTHIC_CRUCIBLE.get());
        BlockEntity blockEntity = helper.getBlockEntity(new BlockPos(0, 1, 0));
        helper.assertTrue(
                blockEntity instanceof MythicCrucibleBlockEntity,
                "the placed block must carry a crucible block entity");
        MythicCrucibleBlockEntity crucible = (MythicCrucibleBlockEntity) blockEntity;
        crucible.inputTank()
                .fill(
                        new FluidStack(Fluids.WATER, MythicCrucibleRecipe.DEFAULT_FLUID_COST_MB),
                        IFluidHandler.FluidAction.EXECUTE);
        crucible.inventory()
                .insertItem(
                        MythicCrucibleBlockEntity.FRAGMENT_SLOT,
                        new ItemStack(ModItems.DIMENSION_FRAGMENTS[0].get()),
                        false);
        BlockPos absolute = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState state = helper.getBlockState(new BlockPos(0, 1, 0));

        int ticks = 40;
        for (int tick = 0; tick < ticks; tick++) {
            MythicCrucibleBlockEntity.serverTick(helper.getLevel(), absolute, state, crucible);
        }

        helper.assertTrue(
                crucible.cycle().status() == MythicCrucibleCycle.Status.RUNNING,
                "a fed crucible must be running");
        helper.assertTrue(
                crucible.cycle().stateTicks() == ticks,
                "the wait timer must advance once per tick, was " + crucible.cycle().stateTicks());
        helper.assertTrue(
                crucible.cycle().elapsedTicks() == 0,
                "refining time must not advance during operation input, was "
                        + crucible.cycle().elapsedTicks());
        helper.assertTrue(
                crucible.cycle().timePenalty() == 0,
                "an empty operation slot must never be penalized");
        helper.succeed();
    }

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
                cycle.status() == MythicCrucibleCycle.Status.REFINING,
                "branch A must enter refining after its operations");
        for (int i = 0; i < cycle.finalResult().timeTicks(); i++) cycle.tick();
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
