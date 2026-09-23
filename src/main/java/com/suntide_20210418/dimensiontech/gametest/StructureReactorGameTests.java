package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.StructureReactorBlockEntity;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipe;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipes;
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
public final class StructureReactorGameTests {
    private StructureReactorGameTests() {}

    /**
     * The tick loop asks the state machine once per tick, and the operation slot is empty on almost
     * all of them. An idle reactor must therefore keep its wait timer running instead of being
     * penalized into a reset every tick.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void anIdleTickLoopKeepsTheWaitTimerRunning(GameTestHelper helper) {
        helper.setBlock(0, 1, 0, ModBlocks.STRUCTURE_REACTOR.get());
        BlockEntity blockEntity = helper.getBlockEntity(new BlockPos(0, 1, 0));
        helper.assertTrue(
                blockEntity instanceof StructureReactorBlockEntity,
                "the placed block must carry a reactor block entity");
        StructureReactorBlockEntity reactor = (StructureReactorBlockEntity) blockEntity;
        reactor.inputTank()
                .fill(
                        new FluidStack(Fluids.WATER, StructureReactorRecipe.DEFAULT_FLUID_COST_MB),
                        IFluidHandler.FluidAction.EXECUTE);
        reactor.inventory()
                .insertItem(
                        StructureReactorBlockEntity.FRAGMENT_SLOT,
                        new ItemStack(ModItems.DIMENSION_FRAGMENTS[0].get()),
                        false);
        BlockPos absolute = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState state = helper.getBlockState(new BlockPos(0, 1, 0));

        int ticks = 40;
        for (int tick = 0; tick < ticks; tick++) {
            StructureReactorBlockEntity.serverTick(helper.getLevel(), absolute, state, reactor);
        }

        helper.assertTrue(
                reactor.cycle().status() == StructureReactorCycle.Status.RUNNING,
                "a fed reactor must be running");
        helper.assertTrue(
                reactor.cycle().stateTicks() == ticks,
                "the wait timer must advance once per tick, was " + reactor.cycle().stateTicks());
        helper.assertTrue(
                reactor.cycle().elapsedTicks() == 0,
                "refining time must not advance during operation input, was "
                        + reactor.cycle().elapsedTicks());
        helper.assertTrue(
                reactor.cycle().timePenalty() == 0,
                "an empty operation slot must never be penalized");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void boundaryInputsConsumeAndResolve(GameTestHelper helper) {
        StructureReactorRecipe<ItemStack> recipe =
                StructureReactorRecipes.all().stream().findFirst().orElseThrow();

        StructureReactorCycle immediate = new StructureReactorCycle<ItemStack>();
        immediate.start(recipe);
        StructureReactorCycle.Resolution result = immediate.resolve(new ItemStack(Items.QUARTZ));
        helper.assertTrue(result.consumesInput(), "an input must be consumed");
        helper.assertTrue(
                result == StructureReactorCycle.Resolution.CORRECT,
                "tick 0 must advance without reward");

        StructureReactorCycle rewarded = new StructureReactorCycle<ItemStack>();
        rewarded.start(recipe);
        for (int i = 0; i < StructureReactorCycle.REWARD_START_TICK; i++) rewarded.tick();
        helper.assertTrue(
                rewarded.resolve(new ItemStack(Items.QUARTZ))
                        == StructureReactorCycle.Resolution.CORRECT_REWARDED,
                "tick 20 must reward");

        StructureReactorCycle<ItemStack> empty = new StructureReactorCycle<>();
        empty.start(recipe);
        empty.tick();
        helper.assertTrue(
                empty.resolve(ItemStack.EMPTY, true) == StructureReactorCycle.Resolution.NONE,
                "empty input must not resolve");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void lateAndWrongInputsUsePenalties(GameTestHelper helper) {
        StructureReactorRecipe<ItemStack> recipe =
                StructureReactorRecipes.all().stream().findFirst().orElseThrow();
        StructureReactorCycle late = new StructureReactorCycle<ItemStack>();
        late.start(recipe);
        for (int i = 0; i <= StructureReactorCycle.STATE_TIMEOUT_TICKS; i++) late.tick();
        StructureReactorCycle.Resolution phase = late.resolve(new ItemStack(Items.QUARTZ));
        helper.assertTrue(
                phase == StructureReactorCycle.Resolution.PHASE_IDLE, "tick 101 must phase-idle");
        helper.assertTrue(phase.consumesInput(), "late input must be consumed");

        StructureReactorCycle wrong = new StructureReactorCycle<ItemStack>();
        wrong.start(recipe);
        StructureReactorCycle.Resolution penalty = wrong.resolve(new ItemStack(Items.REDSTONE));
        helper.assertTrue(penalty.consumesInput(), "wrong input must be consumed");
        helper.assertTrue(
                penalty == StructureReactorCycle.Resolution.STABILIZE_FAILURE,
                "stabilize at branch must use its specific penalty");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 20)
    public static void fifthTierAlternatesOnlyAfterCommit(GameTestHelper helper) {
        StructureReactorRecipe<ItemStack> recipe =
                StructureReactorRecipes.all().stream()
                        .filter(StructureReactorRecipe::hasAlternateBranch)
                        .findFirst()
                        .orElseThrow();
        StructureReactorCycle cycle = new StructureReactorCycle<ItemStack>();
        cycle.start(recipe);
        resolve(cycle, Items.QUARTZ);
        resolve(cycle, Items.AMETHYST_SHARD);
        resolve(cycle, Items.AMETHYST_SHARD);
        resolve(cycle, Items.GLOWSTONE_DUST);
        resolve(cycle, Items.REDSTONE);
        helper.assertTrue(
                cycle.status() == StructureReactorCycle.Status.REFINING,
                "branch A must enter refining after its operations");
        for (int i = 0; i < cycle.finalResult().timeTicks(); i++) cycle.tick();
        helper.assertTrue(
                cycle.status() == StructureReactorCycle.Status.READY_TO_COMMIT,
                "branch A must reach commit state");
        helper.assertTrue(
                cycle.branch() == StructureReactorRecipe.Branch.A,
                "branch must not change before commit");
        cycle.commit();
        helper.assertTrue(
                cycle.branch() == StructureReactorRecipe.Branch.B,
                "branch must change once after commit");
        helper.succeed();
    }

    private static void resolve(StructureReactorCycle<ItemStack> cycle, Item item) {
        cycle.resolve(new ItemStack(item));
    }
}
