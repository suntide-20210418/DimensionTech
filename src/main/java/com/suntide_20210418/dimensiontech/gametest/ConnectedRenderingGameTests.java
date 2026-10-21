package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.AssemblyPanelBlock;
import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.StructureMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.StructureMinerVisuals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Server integration tests: static visuals must follow real formation/lifecycle. They run only
 * under the Forge GameTest launcher, not on a normal game server.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConnectedRenderingGameTests {
    private static BlockPos build(GameTestHelper helper) {
        BlockPos owner = helper.absolutePos(new BlockPos(15, 9, 15));
        helper.getLevel()
                .setBlock(
                        owner,
                        ModBlocks.TIER_1_STRUCTURE_MINER.get().defaultBlockState(),
                        Block.UPDATE_ALL);
        StructureMinerMultiblock.place(helper.getLevel(), owner);
        return owner;
    }

    private static void assertSlots(GameTestHelper helper, BlockPos owner, boolean formed) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(
                level.getBlockState(owner).getValue(BaseMinerBlock.ASSEMBLED) == formed,
                "controller formation flag");
        for (var part : StructureMinerMultiblock.projection()) {
            BlockPos pos = owner.offset(part.offset());
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(AssemblyPanelBlock.ASSEMBLY_SLOT)) continue;
            int slot = state.getValue(AssemblyPanelBlock.ASSEMBLY_SLOT);
            helper.assertTrue(
                    formed ? slot > 0 : slot == 0, "part formation flag at " + part.offset());
            if (slot > 0)
                helper.assertTrue(
                        pos.subtract(StructureMinerVisuals.offset(slot)).equals(owner),
                        "stable slot owner");
        }
    }

    @GameTest(template = "redwedge_empty", timeoutTicks = 80)
    public static void formedRigAssignsEveryPart(GameTestHelper helper) {
        BlockPos owner = build(helper);
        helper.runAfterDelay(
                4,
                () -> {
                    assertSlots(helper, owner, true);
                    helper.succeed();
                });
    }

    @GameTest(template = "redwedge_empty", timeoutTicks = 80)
    public static void breakingRigClearsVisualState(GameTestHelper helper) {
        BlockPos owner = build(helper);
        helper.runAfterDelay(4, () -> helper.getLevel().removeBlock(owner.offset(2, -1, 2), false));
        helper.runAfterDelay(
                7,
                () -> {
                    assertSlots(helper, owner, false);
                    helper.succeed();
                });
    }

    @GameTest(template = "redwedge_empty", timeoutTicks = 80)
    public static void removingControllerClearsLoadedParts(GameTestHelper helper) {
        BlockPos owner = build(helper);
        helper.runAfterDelay(4, () -> helper.getLevel().removeBlock(owner, false));
        helper.runAfterDelay(
                6,
                () -> {
                    for (var part : StructureMinerMultiblock.projection()) {
                        BlockState state =
                                helper.getLevel().getBlockState(owner.offset(part.offset()));
                        helper.assertTrue(
                                state.getValue(AssemblyPanelBlock.ASSEMBLY_SLOT) == 0,
                                "orphaned formed part");
                    }
                    helper.succeed();
                });
    }

    @GameTest(template = "redwedge_empty", timeoutTicks = 80)
    public static void replacingAnUpgradeReattachesItsVisualState(GameTestHelper helper) {
        BlockPos owner = build(helper);
        helper.runAfterDelay(
                4,
                () ->
                        helper.getLevel()
                                .setBlock(
                                        owner.offset(0, -3, -1),
                                        ModBlocks.UPGRADE_ENERGY_TIERS[5].get().defaultBlockState(),
                                        Block.UPDATE_ALL));
        helper.runAfterDelay(
                7,
                () -> {
                    assertSlots(helper, owner, true);
                    helper.succeed();
                });
    }

    @GameTest(template = "redwedge_empty", timeoutTicks = 80)
    public static void formedStateCrossesChunkBoundaries(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos owner =
                new BlockPos(
                        ((origin.getX() + 24) >> 4) << 4,
                        origin.getY() + 9,
                        ((origin.getZ() + 24) >> 4) << 4);
        helper.getLevel()
                .setBlock(
                        owner,
                        ModBlocks.TIER_6_STRUCTURE_MINER.get().defaultBlockState(),
                        Block.UPDATE_ALL);
        StructureMinerMultiblock.place(helper.getLevel(), owner);
        helper.runAfterDelay(
                4,
                () -> {
                    assertSlots(helper, owner, true);
                    helper.succeed();
                });
    }

    @GameTest(template = "redwedge_empty", timeoutTicks = 20)
    public static void glassCullsSharedFacesAcrossFormationStates(GameTestHelper helper) {
        Block glass = ModBlocks.STRUCTURE_MINER_GLASS.get();
        BlockState loose = glass.defaultBlockState(),
                formed = loose.setValue(AssemblyPanelBlock.ASSEMBLY_SLOT, 1);
        for (Direction side : Direction.values()) {
            helper.assertTrue(
                    glass.skipRendering(loose, formed, side), "glass internal face " + side);
            helper.assertTrue(
                    !glass.skipRendering(
                            loose,
                            ModBlocks.STRUCTURE_MINER_CASING.get().defaultBlockState(),
                            side),
                    "glass/casing boundary");
        }
        helper.succeed();
    }

    @GameTest(template = "redwedge_empty", timeoutTicks = 20)
    public static void visualRefreshNeverLoadsFarChunks(GameTestHelper helper) {
        BlockPos far = new BlockPos(20_000_000, 80, 20_000_000);
        helper.assertTrue(!helper.getLevel().hasChunkAt(far), "fixture must start unloaded");
        helper.assertTrue(
                !StructureMinerVisuals.refresh(helper.getLevel(), far, true), "refresh must defer");
        helper.assertTrue(!helper.getLevel().hasChunkAt(far), "refresh force-loaded a chunk");
        helper.succeed();
    }
}
