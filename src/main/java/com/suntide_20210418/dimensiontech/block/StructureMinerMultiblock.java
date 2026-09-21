package com.suntide_20210418.dimensiontech.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Derrick-style tower: the miner head owns the top layer and the machine hangs five layers below it.
 *
 * <p>The head keeps its four side neighbours and the block above it free, so automation can attach
 * to any of those five faces without routing around the frame. The body below is a 5x5 tower: a neck
 * collar that carries the head, three stacked module bays per face, and a 3x3 boring head at the
 * bottom with the drill point in the middle.
 */
public class StructureMinerMultiblock {
    private static final Set<String> CASING =
            Set.of(
                    "0,-1,-2",
                    "-1,-1,-1",
                    "0,-1,-1",
                    "1,-1,-1",
                    "-2,-1,0",
                    "-1,-1,0",
                    "1,-1,0",
                    "2,-1,0",
                    "-1,-1,1",
                    "0,-1,1",
                    "1,-1,1",
                    "0,-1,2",
                    "-1,-3,-1",
                    "0,-3,-1",
                    "1,-3,-1",
                    "-1,-3,0",
                    "1,-3,0",
                    "-1,-3,1",
                    "0,-3,1",
                    "1,-3,1",
                    "-1,-4,-1",
                    "0,-4,-1",
                    "1,-4,-1",
                    "-1,-4,0",
                    "1,-4,0",
                    "-1,-4,1",
                    "0,-4,1",
                    "1,-4,1",
                    "0,-5,-2",
                    "-2,-5,0",
                    "2,-5,0",
                    "0,-5,2");
    private static final Set<String> FOCUS =
            Set.of(
                    "-1,-5,-1",
                    "0,-5,-1",
                    "1,-5,-1",
                    "-1,-5,0",
                    "1,-5,0",
                    "-1,-5,1",
                    "0,-5,1",
                    "1,-5,1");
    private static final Set<String> STRUCTURE = Set.of("0,-1,0", "0,-5,0");

    /**
     * The twelve module bays: four face-centred columns of three stacked slots (y = -2, -3, -4).
     * Each accepts a miner upgrade block or the structure block, and the build button fills empty
     * ones with the structure block, so a fresh player can complete the multiblock out of the shared
     * structure recipe alone without needing any tier-gated machine product.
     */
    private static final Set<String> UPGRADE =
            Set.of(
                    "0,-2,-2",
                    "-2,-2,0",
                    "2,-2,0",
                    "0,-2,2",
                    "0,-3,-2",
                    "-2,-3,0",
                    "2,-3,0",
                    "0,-3,2",
                    "0,-4,-2",
                    "-2,-4,0",
                    "2,-4,0",
                    "0,-4,2");

    private StructureMinerMultiblock() {}

    public enum ProjectionKind {
        CASING,
        FOCUS,
        STRUCTURE,
        UPGRADE
    }

    public record ProjectionBlock(BlockPos offset, BlockState state, ProjectionKind kind) {}

    /**
     * What the build button still has to place. {@code required} holds one entry per missing block;
     * positions that already hold the right block are omitted, so a repeated press costs nothing.
     * {@code blocked} lists positions holding an unrelated, non-replaceable block.
     */
    public record BuildPlan(Map<Block, Integer> required, List<BlockPos> blocked) {
        public boolean isClear() {
            return blocked.isEmpty();
        }

        public boolean isSatisfied() {
            return blocked.isEmpty() && required.isEmpty();
        }
    }

    public static List<ProjectionBlock> projection(int tier) {
        BlockState casing = ModBlocks.STRUCTURE_MINER_CASING.get().defaultBlockState();
        BlockState structure = ModBlocks.STRUCTURE_MINER_STRUCTURE.get().defaultBlockState();
        BlockState focus =
                ModBlocks.DIMENSION_FOCUS[Math.max(0, Math.min(5, tier - 1))]
                        .get()
                        .defaultBlockState();
        List<ProjectionBlock> blocks = new ArrayList<>(54);
        addProjection(blocks, CASING, casing, ProjectionKind.CASING);
        addProjection(blocks, FOCUS, focus, ProjectionKind.FOCUS);
        addProjection(blocks, STRUCTURE, structure, ProjectionKind.STRUCTURE);
        addProjection(blocks, UPGRADE, structure, ProjectionKind.UPGRADE);
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

    public static List<StructureMinerUpgradeBlock> upgrades(ServerLevel level, BlockPos center) {
        List<StructureMinerUpgradeBlock> blocks = new ArrayList<>(UPGRADE.size());
        for (String coordinate : UPGRADE) {
            String[] components = coordinate.split(",");
            BlockPos offset =
                    new BlockPos(
                            Integer.parseInt(components[0]),
                            Integer.parseInt(components[1]),
                            Integer.parseInt(components[2]));
            if (level.getBlockState(center.offset(offset)).getBlock()
                    instanceof StructureMinerUpgradeBlock upgrade) {
                blocks.add(upgrade);
            }
        }
        return List.copyOf(blocks);
    }

    /** Read-only material plan for the build button; never mutates the level. */
    public static BuildPlan planMaterials(ServerLevel level, BlockPos center, int tier) {
        Map<Block, Integer> required = new LinkedHashMap<>();
        List<BlockPos> blocked = new ArrayList<>();
        for (ProjectionBlock projected : projection(tier)) {
            BlockPos target = center.offset(projected.offset());
            BlockState actual = level.getBlockState(target);
            Block wanted = projected.state().getBlock();
            if (actual.is(wanted)) continue;
            if (!actual.isAir() && !actual.canBeReplaced()) {
                blocked.add(target);
                continue;
            }
            required.merge(wanted, 1, Integer::sum);
        }
        return new BuildPlan(Map.copyOf(required), List.copyOf(blocked));
    }

    /** Whether the given block state may occupy the upgrade slots of the multiblock. */
    public static boolean acceptsUpgradeSlot(BlockState state) {
        return state.getBlock() instanceof StructureMinerUpgradeBlock
                || state.is(ModBlocks.STRUCTURE_MINER_STRUCTURE.get());
    }

    public static boolean isComplete(ServerLevel level, BlockPos center, int tier) {
        BlockState casing = ModBlocks.STRUCTURE_MINER_CASING.get().defaultBlockState();
        BlockState structure = ModBlocks.STRUCTURE_MINER_STRUCTURE.get().defaultBlockState();
        BlockState focus =
                ModBlocks.DIMENSION_FOCUS[Math.max(0, Math.min(5, tier - 1))]
                        .get()
                        .defaultBlockState();
        for (int x = -2; x <= 2; x++)
            for (int y = -5; y <= -1; y++)
                for (int z = -2; z <= 2; z++) {
                    String key = x + "," + y + "," + z;
                    BlockState actual = level.getBlockState(center.offset(x, y, z));
                    if (CASING.contains(key) && !actual.is(casing.getBlock())) return false;
                    if (FOCUS.contains(key) && !actual.is(focus.getBlock())) return false;
                    if (STRUCTURE.contains(key) && !actual.is(structure.getBlock())) return false;
                    if (UPGRADE.contains(key) && !acceptsUpgradeSlot(actual)) return false;
                }
        return true;
    }

    public static void place(ServerLevel level, BlockPos center) {
        place(level, center, 1);
    }

    public static void place(ServerLevel level, BlockPos center, int tier) {
        for (ProjectionBlock projected : projection(tier)) {
            level.setBlock(
                    center.offset(projected.offset()), projected.state(), Block.UPDATE_ALL);
        }
    }
}
