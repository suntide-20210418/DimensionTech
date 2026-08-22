package com.suntide_20210418.dimensiontech.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Coordinate-faithful structure adapted from the supplied Modular Machinery definition. */
public final class MythicMinerMultiblock {
    private static final Set<String> CASING =
            Set.of(
                    "-2,0,2",
                    "-3,0,0",
                    "-2,0,-2",
                    "3,0,0",
                    "2,0,2",
                    "3,-1,0",
                    "2,-1,-2",
                    "1,-1,-2",
                    "-3,-1,1",
                    "-2,-1,1",
                    "-1,-1,-2",
                    "-3,-1,0",
                    "-2,-1,-2",
                    "-1,-1,3",
                    "2,-1,1",
                    "3,-1,-1",
                    "2,-1,-1",
                    "1,-1,-3",
                    "0,-1,-3",
                    "-2,-1,-1",
                    "-2,-1,2",
                    "1,-1,2",
                    "2,-1,2",
                    "0,-1,3",
                    "3,-1,1",
                    "1,-1,3",
                    "0,0,-3",
                    "0,0,3",
                    "-1,-1,-3",
                    "-3,-1,-1",
                    "-1,-1,2",
                    "2,0,-2");
    private static final Set<String> FOCUS =
            Set.of("-3,1,0", "3,1,0", "-2,1,2", "2,1,2", "0,1,-3", "2,1,-2", "0,1,3", "-2,1,-2");
    private static final Set<String> STRUCTURE = Set.of("0,-1,0");

    /** The twelve non-frame, non-focus parts from the source definition become upgrade slots. */
    private static final Set<String> UPGRADE =
            Set.of(
                    "2,-1,0",
                    "1,-1,0",
                    "-1,-1,0",
                    "0,-1,-2",
                    "-2,-1,0",
                    "-1,-1,1",
                    "0,-1,1",
                    "1,-1,1",
                    "1,-1,-1",
                    "0,-1,-1",
                    "-1,-1,-1",
                    "0,-1,2");

    private MythicMinerMultiblock() {}

    public enum ProjectionKind {
        CASING,
        FOCUS,
        STRUCTURE,
        UPGRADE
    }

    public record ProjectionBlock(BlockPos offset, BlockState state, ProjectionKind kind) {}

    public static List<ProjectionBlock> projection(int tier) {
        BlockState casing = ModBlocks.MYTHIC_MINER_CASING.get().defaultBlockState();
        BlockState structure = ModBlocks.MYTHIC_MINER_STRUCTURE.get().defaultBlockState();
        BlockState focus =
                ModBlocks.DIMENSION_FOCUS[Math.max(0, Math.min(5, tier - 1))]
                        .get()
                        .defaultBlockState();
        BlockState upgrade = ModBlocks.UPGRADE_NONE.get().defaultBlockState();
        List<ProjectionBlock> blocks = new ArrayList<>(53);
        addProjection(blocks, CASING, casing, ProjectionKind.CASING);
        addProjection(blocks, FOCUS, focus, ProjectionKind.FOCUS);
        addProjection(blocks, STRUCTURE, structure, ProjectionKind.STRUCTURE);
        addProjection(blocks, UPGRADE, upgrade, ProjectionKind.UPGRADE);
        return List.copyOf(blocks);
    }

    private static void addProjection(
            List<ProjectionBlock> blocks,
            Set<String> coordinates,
            BlockState state,
            ProjectionKind kind) {
        for (String coordinate : coordinates) {
            String[] components = coordinate.split(",");
            blocks.add(
                    new ProjectionBlock(
                            new BlockPos(
                                    Integer.parseInt(components[0]),
                                    Integer.parseInt(components[1]),
                                    Integer.parseInt(components[2])),
                            state,
                            kind));
        }
    }

    public static List<MythicMinerUpgradeBlock> upgrades(ServerLevel level, BlockPos center) {
        List<MythicMinerUpgradeBlock> blocks = new ArrayList<>(UPGRADE.size());
        for (String coordinate : UPGRADE) {
            String[] components = coordinate.split(",");
            BlockPos offset =
                    new BlockPos(
                            Integer.parseInt(components[0]),
                            Integer.parseInt(components[1]),
                            Integer.parseInt(components[2]));
            if (level.getBlockState(center.offset(offset)).getBlock()
                    instanceof MythicMinerUpgradeBlock upgrade) {
                blocks.add(upgrade);
            }
        }
        return List.copyOf(blocks);
    }

    public static boolean isComplete(ServerLevel level, BlockPos center, int tier) {
        BlockState casing = ModBlocks.MYTHIC_MINER_CASING.get().defaultBlockState();
        BlockState structure = ModBlocks.MYTHIC_MINER_STRUCTURE.get().defaultBlockState();
        BlockState focus =
                ModBlocks.DIMENSION_FOCUS[Math.max(0, Math.min(5, tier - 1))]
                        .get()
                        .defaultBlockState();
        for (int x = -3; x <= 3; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -3; z <= 3; z++) {
                    String key = x + "," + y + "," + z;
                    BlockState actual = level.getBlockState(center.offset(x, y, z));
                    if (CASING.contains(key) && !actual.is(casing.getBlock())) return false;
                    if (FOCUS.contains(key) && !actual.is(focus.getBlock())) return false;
                    if (STRUCTURE.contains(key) && !actual.is(structure.getBlock())) return false;
                    if (UPGRADE.contains(key)
                            && !(actual.getBlock() instanceof MythicMinerUpgradeBlock))
                        return false;
                }
        return true;
    }

    public static void place(ServerLevel level, BlockPos center) {
        place(level, center, 1);
    }

    public static void place(ServerLevel level, BlockPos center, int tier) {
        BlockState casing = ModBlocks.MYTHIC_MINER_CASING.get().defaultBlockState();
        BlockState structure = ModBlocks.MYTHIC_MINER_STRUCTURE.get().defaultBlockState();
        BlockState focus =
                ModBlocks.DIMENSION_FOCUS[Math.max(0, Math.min(5, tier - 1))]
                        .get()
                        .defaultBlockState();
        BlockState none = ModBlocks.UPGRADE_NONE.get().defaultBlockState();
        for (int x = -3; x <= 3; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -3; z <= 3; z++) {
                    String key = x + "," + y + "," + z;
                    if (!CASING.contains(key)
                            && !FOCUS.contains(key)
                            && !STRUCTURE.contains(key)
                            && !UPGRADE.contains(key)) continue;
                    BlockState state =
                            FOCUS.contains(key)
                                    ? focus
                                    : CASING.contains(key)
                                            ? casing
                                            : STRUCTURE.contains(key) ? structure : none;
                    BlockPos target = center.offset(x, y, z);
                    level.setBlock(target, state, Block.UPDATE_ALL);
                }
    }
}
