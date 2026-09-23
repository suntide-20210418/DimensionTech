package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.StructureReactorBlockEntity;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipe;
import com.suntide_20210418.dimensiontech.structurereactor.StateId;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Resource transaction coverage for the reactor block entity: startup reservation, the commit that
 * charges the final cost and refunds the rest, a blocked output that must not charge anything, and
 * block destruction that returns everything the cycle reserved.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureReactorTransactionGameTests {
    private static final BlockPos REACTOR = new BlockPos(1, 2, 1);
    private static final int RESERVED_FLUID = 2_000;
    private static final int RESERVED_FRAGMENTS = 2;
    private static final int BASE_FLUID = 1_000;
    private static final int TARGET_OUTPUT = 1_000;

    private StructureReactorTransactionGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void startupReservesFluidAndFragments(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = placeStarted(helper);

        helper.assertTrue(
                reactor.inputTank().getFluidAmount() == RESERVED_FLUID - BASE_FLUID,
                "startup must move the base fluid into the reservation but left "
                        + reactor.inputTank().getFluidAmount());
        helper.assertTrue(
                fragmentCount(reactor) == RESERVED_FRAGMENTS - 1,
                "startup must move the base fragment into the reservation but left "
                        + fragmentCount(reactor));
        helper.assertTrue(
                reactor.cycle().recipe() != null,
                "a matching fluid, fragment and output space must start a cycle");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void completionChargesTheFinalCostAndRefundsTheRest(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = placeStarted(helper);
        StructureReactorRecipe<ItemStack> recipe = reactor.cycle().recipe();
        int finalFluid = runUnrewardedCycleToCommit(helper, reactor);

        helper.assertTrue(
                finalFluid == BASE_FLUID,
                "without rewards the final cost must equal the base cost but was " + finalFluid);
        helper.assertTrue(
                reactor.cycle().status() == StructureReactorCycle.Status.IDLE,
                "a committed cycle must return to idle");
        helper.assertTrue(
                reactor.inputTank().getFluidAmount() == RESERVED_FLUID - finalFluid,
                "the input tank must keep exactly the consumed fluid but holds "
                        + reactor.inputTank().getFluidAmount());
        helper.assertTrue(
                fragmentCount(reactor) == RESERVED_FRAGMENTS,
                "the unused reserved fragment must return to its slot but left "
                        + fragmentCount(reactor));
        helper.assertTrue(
                reactor.outputTank().getFluidAmount() == TARGET_OUTPUT
                        && reactor.outputTank().getFluid().getFluid().isSame(recipe.output()),
                "the final product must be delivered to the output tank");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void blockedOutputHoldsCommitWithoutCharging(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = placeStarted(helper);
        reactor.outputTank()
                .fill(
                        new FluidStack(Fluids.LAVA, 16_000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        runUnrewardedCycleToReady(helper, reactor);
        helper.assertTrue(
                reactor.cycle().status() == StructureReactorCycle.Status.READY_TO_COMMIT,
                "a full output tank must keep the finished cycle waiting");
        helper.assertTrue(
                reactor.inputTank().getFluidAmount() == RESERVED_FLUID - BASE_FLUID,
                "a blocked commit must not charge anything yet but charged "
                        + (RESERVED_FLUID - reactor.inputTank().getFluidAmount()));
        helper.succeed();
    }

    /**
     * Block replacement runs the return path before the entity is discarded, so this drives the
     * same hook with the block still present and checks the reserved resources land back inside.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void abortReturnsReservedResourcesAndClearsTheCycle(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = placeStarted(helper);
        reactor.abortAndReturnResources();

        helper.assertTrue(
                reactor.inputTank().getFluidAmount() == RESERVED_FLUID,
                "aborting must return the reserved fluid but left "
                        + reactor.inputTank().getFluidAmount());
        helper.assertTrue(
                fragmentCount(reactor) == RESERVED_FRAGMENTS,
                "aborting must return the reserved fragment but left " + fragmentCount(reactor));
        helper.assertTrue(
                reactor.cycle().status() == StructureReactorCycle.Status.IDLE,
                "aborting must clear the running cycle");
        helper.assertTrue(reactor.cycle().recipe() == null, "aborting must forget the recipe");
        helper.assertTrue(
                reactor.cycle().carriedExtraRecursion() == 0,
                "a recipe change must clear the carried recursion");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void breakingTheBlockDropsTheOperationCache(GameTestHelper helper) {
        StructureReactorBlockEntity reactor = placeStarted(helper);
        helper.getLevel().removeBlock(helper.absolutePos(REACTOR), false);

        helper.assertTrue(
                reactor.cycle().status() == StructureReactorCycle.Status.IDLE,
                "breaking must clear the running cycle");
        helper.assertTrue(
                fragmentCount(reactor) == 0, "breaking must empty the cached fragment slot");
        helper.assertTrue(
                entityCount(helper) >= 1,
                "breaking must drop the cached fragments and operation items as entities");
        helper.succeed();
    }

    private static int runUnrewardedCycleToReady(
            GameTestHelper helper, StructureReactorBlockEntity reactor) {
        StructureReactorCycle cycle = reactor.cycle();
        while (cycle.status() == StructureReactorCycle.Status.RUNNING) {
            cycle.resolve(new ItemStack(itemFor(cycle.currentState())));
        }
        while (cycle.status() == StructureReactorCycle.Status.REFINING) cycle.tick();
        return cycle.finalResult().fluidCostMb();
    }

    private static int runUnrewardedCycleToCommit(
            GameTestHelper helper, StructureReactorBlockEntity reactor) {
        int finalFluid = runUnrewardedCycleToReady(helper, reactor);
        StructureReactorBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(REACTOR),
                helper.getBlockState(REACTOR),
                reactor);
        return finalFluid;
    }

    private static StructureReactorBlockEntity placeStarted(GameTestHelper helper) {
        helper.setBlock(REACTOR, ModBlocks.STRUCTURE_REACTOR.get());
        StructureReactorBlockEntity reactor =
                (StructureReactorBlockEntity) helper.getBlockEntity(REACTOR);
        helper.assertTrue(reactor != null, "the reactor block entity must exist");
        reactor.inputTank()
                .fill(
                        new FluidStack(Fluids.WATER, RESERVED_FLUID),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        reactor.inventory()
                .insertItem(
                        StructureReactorBlockEntity.FRAGMENT_SLOT,
                        new ItemStack(ModItems.DIMENSION_FRAGMENTS[0].get(), RESERVED_FRAGMENTS),
                        false);
        reactor.inventory()
                .insertItem(
                        StructureReactorBlockEntity.OPERATION_SLOT,
                        new ItemStack(Items.QUARTZ, 4),
                        false);
        StructureReactorBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(REACTOR),
                helper.getBlockState(REACTOR),
                reactor);
        helper.assertTrue(
                reactor.cycle().status() != StructureReactorCycle.Status.IDLE,
                "the reactor must start once the input fluid is present");
        return reactor;
    }

    private static int fragmentCount(StructureReactorBlockEntity reactor) {
        return reactor.inventory()
                .getStackInSlot(StructureReactorBlockEntity.FRAGMENT_SLOT)
                .getCount();
    }

    private static int entityCount(GameTestHelper helper) {
        return helper.getLevel()
                .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(REACTOR)).inflate(2))
                .size();
    }

    private static Item itemFor(StateId state) {
        return switch (state) {
            case BRANCH -> Items.QUARTZ;
            case RECURSE -> Items.AMETHYST_SHARD;
            case CONVERGE -> Items.GLOWSTONE_DUST;
            case STABILIZE -> Items.REDSTONE;
        };
    }
}
