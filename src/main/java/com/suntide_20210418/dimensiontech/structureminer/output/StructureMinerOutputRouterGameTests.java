package com.suntide_20210418.dimensiontech.structureminer.output;

import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.mojang.logging.LogUtils;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/** Integration boundary checks for the ordinary item-handler output route. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureMinerOutputRouterGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Mirrors the guard in {@link StructureMinerOutputRouter}: AE2 may be absent at runtime. */
    private static final boolean AE2_LOADED = ModList.get().isLoaded("ae2");

    private StructureMinerOutputRouterGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void routesLootIntoAnEnabledAdjacentContainer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos minerPosition = helper.absolutePos(BlockPos.ZERO);
        BlockPos chestPosition = minerPosition.relative(Direction.NORTH);
        level.setBlock(chestPosition, Blocks.CHEST.defaultBlockState(), 3);

        List<ItemStack> remainder =
                StructureMinerOutputRouter.output(
                        level,
                        minerPosition,
                        false,
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
        if (!AE2_LOADED) {
            /*
             * -PvanillaLootRuntime=true 把可选模组降为 compileOnly（build.gradle:20-24），AE2 此时不在运行期
             * 类路径上，本用例没有可测对象。显式跳过而不是让它以 NoClassDefFoundError 失败：后者只会报出
             * "appeng.core.definitions.AEBlocks" 这种环境缺失的假象，把真正的 AE2 集成回归掩盖在同一行日志里。
             */
            LOGGER.info(
                    "[gametest] ae2 is not on the runtime classpath; {} is not applicable",
                    "routesLootIntoAnOnlineAe2Interface");
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos routerPosition = helper.absolutePos(BlockPos.ZERO);
        BlockPos interfacePosition = routerPosition.relative(Direction.NORTH);
        BlockPos controllerPosition = interfacePosition.relative(Direction.EAST);
        BlockPos energyPosition = controllerPosition.relative(Direction.EAST);
        BlockPos chestPosition = interfacePosition.relative(Direction.WEST);
        level.setBlock(interfacePosition, AEBlocks.INTERFACE.block().defaultBlockState(), 3);
        level.setBlock(controllerPosition, AEBlocks.CONTROLLER.block().defaultBlockState(), 3);
        level.setBlock(
                energyPosition, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(), 3);
        level.setBlock(chestPosition, AEBlocks.ME_CHEST.block().defaultBlockState(), 3);
        if (!(level.getBlockEntity(chestPosition)
                instanceof appeng.blockentity.storage.MEChestBlockEntity chest)) {
            helper.fail("AE2 chest did not create its block entity");
            return;
        }
        chest.setCell(AEItems.ITEM_CELL_1K.stack());

        helper.runAfterDelay(
                40,
                () -> {
                    if (!(level.getBlockEntity(interfacePosition)
                                    instanceof InterfaceBlockEntity interfaceBlock)
                            || !interfaceBlock.getMainNode().isOnline()) {
                        helper.fail("AE2 interface did not join the test network");
                        return;
                    }
                    List<ItemStack> remainder =
                            StructureMinerOutputRouter.output(
                                    level,
                                    routerPosition,
                                    true,
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
