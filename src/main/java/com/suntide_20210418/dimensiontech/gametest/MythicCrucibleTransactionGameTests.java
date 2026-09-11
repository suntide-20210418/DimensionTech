package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipe;
import com.suntide_20210418.dimensiontech.mythiccrucible.StateId;
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
 * Resource transaction coverage for the crucible block entity: startup reservation, the commit that
 * charges the final cost and refunds the rest, a blocked output that must not charge anything, and
 * block destruction that returns everything the cycle reserved.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicCrucibleTransactionGameTests {
    private static final BlockPos CRUCIBLE = new BlockPos(1, 2, 1);
    private static final int RESERVED_FLUID = 2_000;
    private static final int RESERVED_FRAGMENTS = 2;
    private static final int BASE_FLUID = 1_000;
    private static final int TARGET_OUTPUT = 1_000;

    private MythicCrucibleTransactionGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void startupReservesFluidAndFragments(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = placeStarted(helper);

        helper.assertTrue(
                crucible.inputTank().getFluidAmount() == RESERVED_FLUID - BASE_FLUID,
                "startup must move the base fluid into the reservation but left "
                        + crucible.inputTank().getFluidAmount());
        helper.assertTrue(
                fragmentCount(crucible) == RESERVED_FRAGMENTS - 1,
                "startup must move the base fragment into the reservation but left "
                        + fragmentCount(crucible));
        helper.assertTrue(
                crucible.cycle().recipe() != null,
                "a matching fluid, fragment and output space must start a cycle");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void completionChargesTheFinalCostAndRefundsTheRest(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = placeStarted(helper);
        MythicCrucibleRecipe<ItemStack> recipe = crucible.cycle().recipe();
        int finalFluid = runUnrewardedCycleToCommit(helper, crucible);

        helper.assertTrue(
                finalFluid == BASE_FLUID,
                "without rewards the final cost must equal the base cost but was " + finalFluid);
        helper.assertTrue(
                crucible.cycle().status() == MythicCrucibleCycle.Status.IDLE,
                "a committed cycle must return to idle");
        helper.assertTrue(
                crucible.inputTank().getFluidAmount() == RESERVED_FLUID - finalFluid,
                "the input tank must keep exactly the consumed fluid but holds "
                        + crucible.inputTank().getFluidAmount());
        helper.assertTrue(
                fragmentCount(crucible) == RESERVED_FRAGMENTS,
                "the unused reserved fragment must return to its slot but left "
                        + fragmentCount(crucible));
        helper.assertTrue(
                crucible.outputTank().getFluidAmount() == TARGET_OUTPUT
                        && crucible.outputTank().getFluid().getFluid().isSame(recipe.output()),
                "the final product must be delivered to the output tank");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void blockedOutputHoldsCommitWithoutCharging(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = placeStarted(helper);
        crucible.outputTank()
                .fill(
                        new FluidStack(Fluids.LAVA, 16_000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        runUnrewardedCycleToReady(helper, crucible);
        helper.assertTrue(
                crucible.cycle().status() == MythicCrucibleCycle.Status.READY_TO_COMMIT,
                "a full output tank must keep the finished cycle waiting");
        helper.assertTrue(
                crucible.inputTank().getFluidAmount() == RESERVED_FLUID - BASE_FLUID,
                "a blocked commit must not charge anything yet but charged "
                        + (RESERVED_FLUID - crucible.inputTank().getFluidAmount()));
        helper.succeed();
    }

    /**
     * Block replacement runs the return path before the entity is discarded, so this drives the
     * same hook with the block still present and checks the reserved resources land back inside.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void abortReturnsReservedResourcesAndClearsTheCycle(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = placeStarted(helper);
        crucible.abortAndReturnResources();

        helper.assertTrue(
                crucible.inputTank().getFluidAmount() == RESERVED_FLUID,
                "aborting must return the reserved fluid but left "
                        + crucible.inputTank().getFluidAmount());
        helper.assertTrue(
                fragmentCount(crucible) == RESERVED_FRAGMENTS,
                "aborting must return the reserved fragment but left " + fragmentCount(crucible));
        helper.assertTrue(
                crucible.cycle().status() == MythicCrucibleCycle.Status.IDLE,
                "aborting must clear the running cycle");
        helper.assertTrue(crucible.cycle().recipe() == null, "aborting must forget the recipe");
        helper.assertTrue(
                crucible.cycle().carriedExtraRecursion() == 0,
                "a recipe change must clear the carried recursion");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void breakingTheBlockDropsTheOperationCache(GameTestHelper helper) {
        MythicCrucibleBlockEntity crucible = placeStarted(helper);
        helper.getLevel().removeBlock(helper.absolutePos(CRUCIBLE), false);

        helper.assertTrue(
                crucible.cycle().status() == MythicCrucibleCycle.Status.IDLE,
                "breaking must clear the running cycle");
        helper.assertTrue(
                fragmentCount(crucible) == 0, "breaking must empty the cached fragment slot");
        helper.assertTrue(
                entityCount(helper) >= 1,
                "breaking must drop the cached fragments and operation items as entities");
        helper.succeed();
    }

    private static int runUnrewardedCycleToReady(
            GameTestHelper helper, MythicCrucibleBlockEntity crucible) {
        MythicCrucibleCycle cycle = crucible.cycle();
        while (cycle.status() == MythicCrucibleCycle.Status.RUNNING) {
            cycle.resolve(new ItemStack(itemFor(cycle.currentState())));
        }
        return cycle.finalResult().fluidCostMb();
    }

    private static int runUnrewardedCycleToCommit(
            GameTestHelper helper, MythicCrucibleBlockEntity crucible) {
        int finalFluid = runUnrewardedCycleToReady(helper, crucible);
        MythicCrucibleBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CRUCIBLE),
                helper.getBlockState(CRUCIBLE),
                crucible);
        return finalFluid;
    }

    private static MythicCrucibleBlockEntity placeStarted(GameTestHelper helper) {
        helper.setBlock(CRUCIBLE, ModBlocks.MYTHIC_CRUCIBLE.get());
        MythicCrucibleBlockEntity crucible =
                (MythicCrucibleBlockEntity) helper.getBlockEntity(CRUCIBLE);
        helper.assertTrue(crucible != null, "the crucible block entity must exist");
        crucible.inputTank()
                .fill(
                        new FluidStack(Fluids.WATER, RESERVED_FLUID),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        crucible.inventory()
                .insertItem(
                        MythicCrucibleBlockEntity.FRAGMENT_SLOT,
                        new ItemStack(ModItems.DIMENSION_FRAGMENTS[0].get(), RESERVED_FRAGMENTS),
                        false);
        crucible.inventory()
                .insertItem(
                        MythicCrucibleBlockEntity.OPERATION_SLOT,
                        new ItemStack(Items.QUARTZ, 4),
                        false);
        MythicCrucibleBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CRUCIBLE),
                helper.getBlockState(CRUCIBLE),
                crucible);
        helper.assertTrue(
                crucible.cycle().status() != MythicCrucibleCycle.Status.IDLE,
                "the crucible must start once the input fluid is present");
        return crucible;
    }

    private static int fragmentCount(MythicCrucibleBlockEntity crucible) {
        return crucible.inventory()
                .getStackInSlot(MythicCrucibleBlockEntity.FRAGMENT_SLOT)
                .getCount();
    }

    private static int entityCount(GameTestHelper helper) {
        return helper.getLevel()
                .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(CRUCIBLE)).inflate(2))
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
