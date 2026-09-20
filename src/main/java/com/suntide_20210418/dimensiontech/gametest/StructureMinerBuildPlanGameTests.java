package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.StructureMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The build button must charge for exactly what is missing.
 *
 * <p>These cover the three properties the cost model depends on: a finished multiblock costs
 * nothing to re-press, an unrelated block is reported instead of overwritten, and a plain casing
 * satisfies an upgrade slot so a first machine never needs a tier-gated part.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureMinerBuildPlanGameTests {
    private StructureMinerBuildPlanGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void completedMultiblockNeedsNothing(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper);
        ServerLevel level = helper.getLevel();
        StructureMinerMultiblock.place(level, miner.getBlockPos(), miner.getMinerTier());

        StructureMinerMultiblock.BuildPlan plan =
                StructureMinerMultiblock.planMaterials(
                        level, miner.getBlockPos(), miner.getMinerTier());
        if (!plan.isSatisfied()) {
            helper.fail("Completed multiblock still asked for " + plan.required());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void onlyTheMissingBlockIsCharged(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper);
        ServerLevel level = helper.getLevel();
        BlockPos center = miner.getBlockPos();
        StructureMinerMultiblock.place(level, center, miner.getMinerTier());
        level.setBlock(center.offset(2, -1, 0), Blocks.AIR.defaultBlockState(), 3);

        StructureMinerMultiblock.BuildPlan plan =
                StructureMinerMultiblock.planMaterials(level, center, miner.getMinerTier());
        Map<Block, Integer> required = plan.required();
        if (!plan.isClear()
                || required.size() != 1
                || required.getOrDefault(ModBlocks.STRUCTURE_MINER_STRUCTURE.get(), 0) != 1) {
            helper.fail("Removing one structure charge ran " + required + " with blocked " + plan.blocked());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void obstructedPositionIsReportedNotOverwritten(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper);
        ServerLevel level = helper.getLevel();
        BlockPos center = miner.getBlockPos();
        StructureMinerMultiblock.place(level, center, miner.getMinerTier());
        BlockPos obstructed = center.offset(-2, -1, 2);
        level.setBlock(obstructed, Blocks.OBSIDIAN.defaultBlockState(), 3);

        StructureMinerMultiblock.BuildPlan plan =
                StructureMinerMultiblock.planMaterials(level, center, miner.getMinerTier());
        if (plan.isClear()
                || plan.blocked().size() != 1
                || !plan.blocked().get(0).equals(obstructed)
                || plan.required().containsKey(Blocks.OBSIDIAN)) {
            helper.fail("Obstruction was not reported cleanly: " + plan.blocked());
            return;
        }
        if (!level.getBlockState(obstructed).is(Blocks.OBSIDIAN)) {
            helper.fail("The obstructing block was overwritten");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void bareMultiblockCostsThirtyTwoCasingsEightFocusAndThirteenStructures(
            GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper);
        ServerLevel level = helper.getLevel();
        BlockPos center = miner.getBlockPos();
        int tier = miner.getMinerTier();
        for (StructureMinerMultiblock.ProjectionBlock projected :
                StructureMinerMultiblock.projection(tier)) {
            level.setBlock(center.offset(projected.offset()), Blocks.AIR.defaultBlockState(), 3);
        }

        Map<Block, Integer> required =
                StructureMinerMultiblock.planMaterials(level, center, tier).required();
        if (required.size() != 3
                || required.getOrDefault(ModBlocks.STRUCTURE_MINER_CASING.get(), 0) != 32
                || required.getOrDefault(ModBlocks.DIMENSION_FOCUS[0].get(), 0) != 8
                || required.getOrDefault(ModBlocks.STRUCTURE_MINER_STRUCTURE.get(), 0) != 13) {
            helper.fail("Unexpected material totals: " + required);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void casingSatisfiesUpgradeSlotsWithoutGrantingBonuses(GameTestHelper helper) {
        BaseMinerBlockEntity miner = placeMiner(helper);
        ServerLevel level = helper.getLevel();
        BlockPos center = miner.getBlockPos();
        StructureMinerMultiblock.place(level, center, miner.getMinerTier());
        BlockPos upgradeSlot = center.offset(2, -1, 0);
        level.setBlock(upgradeSlot, ModBlocks.STRUCTURE_MINER_CASING.get().defaultBlockState(), 3);
        BlockState state = level.getBlockState(upgradeSlot);

        if (!state.is(ModBlocks.STRUCTURE_MINER_CASING.get())
                || !StructureMinerMultiblock.acceptsUpgradeSlot(state)) {
            helper.fail("Upgrade slot did not accept a casing");
            return;
        }
        if (!StructureMinerMultiblock.upgrades(level, center).isEmpty()) {
            helper.fail("Casings leaked into the upgrade bonus list");
            return;
        }
        if (StructureMinerMultiblock.acceptsUpgradeSlot(Blocks.OBSIDIAN.defaultBlockState())) {
            helper.fail("An unrelated block was accepted in an upgrade slot");
            return;
        }
        miner.serverTick();
        if (!miner.isStructureComplete()) {
            helper.fail("A casing-filled multiblock did not count as complete");
            return;
        }
        helper.succeed();
    }

    private static BaseMinerBlockEntity placeMiner(GameTestHelper helper) {
        BlockPos position = helper.absolutePos(BlockPos.ZERO);
        ServerLevel level = helper.getLevel();
        level.setBlock(
                position, ModBlocks.TIER_1_STRUCTURE_MINER.get().defaultBlockState(), 3);
        if (level.getBlockEntity(position) instanceof BaseMinerBlockEntity miner) return miner;
        throw new IllegalStateException("Miner did not create a block entity");
    }
}
