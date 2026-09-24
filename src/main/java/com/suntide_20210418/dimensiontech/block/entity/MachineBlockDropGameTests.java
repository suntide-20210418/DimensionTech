package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Breaking a machine must not destroy what the player put inside it.
 *
 * <p>The stored items are markers, and a marker carries the entire analysis payload — a silently
 * deleted one is a real loss of work rather than a cosmetic one, which is exactly why {@code
 * StructureReactorBlock} already returned its contents from {@code onRemove}. These tests pin the
 * same contract for the two consoles that were missing it.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineBlockDropGameTests {
    private MachineBlockDropGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void breakingStructureMinerDropsItsStoredMarkers(GameTestHelper helper) {
        BlockPos position = new BlockPos(2, 2, 2);
        helper.setBlock(position, ModBlocks.TIER_1_STRUCTURE_MINER.get());
        if (!(helper.getBlockEntity(position) instanceof BaseMinerBlockEntity miner)) {
            helper.fail("Structure miner did not create its block entity");
            return;
        }
        if (!miner.getItemHandler()
                .insertItem(0, new ItemStack(ModItems.STRUCTURE_MARKER.get()), false)
                .isEmpty()) {
            helper.fail("Structure miner refused a marker in its first slot");
        }
        helper.destroyBlock(position);
        helper.assertItemEntityPresent(ModItems.STRUCTURE_MARKER.get(), position, 3.0D);
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void breakingStructureDataOperatorDropsItsStoredMarkers(GameTestHelper helper) {
        BlockPos position = new BlockPos(2, 2, 2);
        helper.setBlock(position, ModBlocks.STRUCTURE_DATA_OPERATOR.get());
        if (!(helper.getBlockEntity(position)
                instanceof StructureDataOperatorBlockEntity operator)) {
            helper.fail("Structure data operator did not create its block entity");
            return;
        }
        if (!operator.inventory()
                .insertItem(
                        StructureDataOperatorBlockEntity.OPERAND_START,
                        new ItemStack(ModItems.STRUCTURE_MARKER.get()),
                        false)
                .isEmpty()) {
            helper.fail("Structure data operator refused a marker in its first write slot");
        }
        helper.destroyBlock(position);
        helper.assertItemEntityPresent(ModItems.STRUCTURE_MARKER.get(), position, 3.0D);
        helper.succeed();
    }
}
