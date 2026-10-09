package com.suntide_20210418.dimensiontech.block.entity;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.mojang.logging.LogUtils;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
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

/** Integration boundary checks for the item output chamber's two ejection paths. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemOutputChamberGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Mirrors the guard in {@code MachineGridNodes}: AE2 may be absent at runtime. */
    private static final boolean AE2_LOADED = ModList.get().isLoaded("ae2");

    private ItemOutputChamberGameTests() {}

    /**
     * The container path moves exactly one group per attempt: the head of the queue leaves and the
     * rest is handed back untouched, so the caller can repeat the call on the next tick.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void ejectsOneGroupPerAttempt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chamberPosition = helper.absolutePos(BlockPos.ZERO);
        BlockPos chestPosition = chamberPosition.relative(Direction.NORTH);
        level.setBlock(chamberPosition, ModBlocks.ITEM_OUTPUT_CHAMBER.get().defaultBlockState(), 3);
        level.setBlock(chestPosition, Blocks.CHEST.defaultBlockState(), 3);
        if (!(level.getBlockEntity(chamberPosition)
                instanceof ItemOutputChamberBlockEntity chamber)) {
            helper.fail("Item output chamber did not create its block entity");
            return;
        }

        List<ItemStack> remainder =
                chamber.eject(
                        level,
                        List.of(new ItemStack(Items.STONE, 4), new ItemStack(Items.DIRT, 3)));

        if (!(level.getBlockEntity(chestPosition) instanceof ChestBlockEntity chest)
                || !chest.getItem(0).is(Items.STONE)
                || chest.getItem(0).getCount() != 4) {
            helper.fail("Item output chamber did not eject the head group into the chest");
            return;
        }
        if (remainder.size() != 1 || !remainder.get(0).is(Items.DIRT)) {
            helper.fail("Item output chamber drained more than one group per attempt");
            return;
        }
        helper.succeed();
    }

    /**
     * With AE2 present the chamber is a device on the network, and an ejection attempt pushes the
     * whole queue into network storage instead of one group.
     */
    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 120)
    public static void pushesWholeQueueIntoTheAe2Network(GameTestHelper helper) {
        if (!AE2_LOADED) {
            /*
             * -PvanillaLootRuntime=true 把可选模组降为 compileOnly（build.gradle），AE2 此时不在运行期类
             * 路径上，本用例没有可测对象。显式跳过而不是让它以 NoClassDefFoundError 失败：后者只会报出
             * "appeng.core.definitions.AEBlocks" 这种环境缺失的假象，把真正的 AE2 集成回归掩盖在同一行日志里。
             */
            LOGGER.info(
                    "[gametest] ae2 is not on the runtime classpath; {} is not applicable",
                    "pushesWholeQueueIntoTheAe2Network");
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos chamberPosition = helper.absolutePos(BlockPos.ZERO);
        BlockPos controllerPosition = chamberPosition.relative(Direction.NORTH);
        BlockPos energyPosition = controllerPosition.relative(Direction.EAST);
        BlockPos chestPosition = controllerPosition.relative(Direction.WEST);
        level.setBlock(chamberPosition, ModBlocks.ITEM_OUTPUT_CHAMBER.get().defaultBlockState(), 3);
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
                    if (!(level.getBlockEntity(chamberPosition)
                            instanceof ItemOutputChamberBlockEntity chamber)) {
                        helper.fail("Item output chamber block entity went missing");
                        return;
                    }
                    if (chamber.gridNode() == null || !chamber.gridNode().isOnline()) {
                        helper.fail("Item output chamber did not join the AE2 network");
                        return;
                    }
                    List<ItemStack> remainder =
                            chamber.eject(
                                    level,
                                    List.of(
                                            new ItemStack(Items.STONE, 4),
                                            new ItemStack(Items.DIRT, 3)));
                    if (!remainder.isEmpty()) {
                        helper.fail("ME ejection left part of the queue behind");
                        return;
                    }
                    helper.succeed();
                });
    }
}
