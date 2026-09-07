package com.suntide_20210418.dimensiontech.block.entity;

import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Integration boundary checks for the ordinary item-handler output route. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicMinerOutputRouterGameTests {
    private MythicMinerOutputRouterGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void routesLootIntoAnEnabledAdjacentContainer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos minerPosition = helper.absolutePos(BlockPos.ZERO);
        BlockPos chestPosition = minerPosition.relative(Direction.NORTH);
        level.setBlock(chestPosition, Blocks.CHEST.defaultBlockState(), 3);

        List<ItemStack> remainder = MythicMinerOutputRouter.output(
                level,
                minerPosition,
                BaseMinerBlockEntity.OutputState.ITEM_HANDLER,
                direction -> direction == Direction.NORTH,
                List.of(new ItemStack(Items.STONE, 4)));
        if (!(level.getBlockEntity(chestPosition) instanceof ChestBlockEntity chest)
                || !remainder.isEmpty()
                || !chest.getItem(0).is(Items.STONE)
                || chest.getItem(0).getCount() != 4) {
            helper.fail("Miner output did not route the full stack into the enabled chest");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void routesLootIntoAnOnlineAe2Interface(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos routerPosition = helper.absolutePos(BlockPos.ZERO);
        BlockPos interfacePosition = routerPosition.relative(Direction.NORTH);
        BlockPos controllerPosition = interfacePosition.relative(Direction.EAST);
        BlockPos energyPosition = controllerPosition.relative(Direction.EAST);
        BlockPos chestPosition = interfacePosition.relative(Direction.WEST);
        level.setBlock(interfacePosition, AEBlocks.INTERFACE.block().defaultBlockState(), 3);
        level.setBlock(controllerPosition, AEBlocks.CONTROLLER.block().defaultBlockState(), 3);
        level.setBlock(energyPosition, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(), 3);
        level.setBlock(chestPosition, AEBlocks.CHEST.block().defaultBlockState(), 3);
        if (!(level.getBlockEntity(chestPosition)
                instanceof appeng.blockentity.storage.ChestBlockEntity chest)) {
            helper.fail("AE2 chest did not create its block entity");
            return;
        }
        chest.setCell(AEItems.ITEM_CELL_1K.stack());

        helper.runAfterDelay(
                40,
                () -> {
                    if (!(level.getBlockEntity(interfacePosition) instanceof InterfaceBlockEntity interfaceBlock)
                            || !interfaceBlock.getMainNode().isOnline()) {
                        helper.fail("AE2 interface did not join the test network");
                        return;
                    }
                    List<ItemStack> remainder =
                            MythicMinerOutputRouter.output(
                                    level,
                                    routerPosition,
                                    BaseMinerBlockEntity.OutputState.ME_NETWORK,
                                    direction -> direction == Direction.NORTH,
                                    List.of(new ItemStack(Items.STONE, 4)));
                    if (!remainder.isEmpty()) {
                        helper.fail("Miner output was not inserted into the online AE2 network");
                        return;
                    }
                    helper.succeed();
                });
    }
}
