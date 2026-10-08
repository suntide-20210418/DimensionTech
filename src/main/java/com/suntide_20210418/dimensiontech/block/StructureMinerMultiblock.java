package com.suntide_20210418.dimensiontech.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drum-style rig: a 5x5 flange plate caps the bottom, a 3x3 glazed waist carries the upgrade bays,
 * and a matching 5x5 flange closes the top. The whole body is a solid 5x5x5 box with the bays
 * carved out of its middle three layers.
 *
 * <p><b>Where the head sits.</b> Every coordinate below is an offset from the controller, which is
 * at {@code (0,0,0)}. The controller occupies the <em>front-left corner of the bottom plate</em>,
 * standing in for the casing block that used to be there: the body then extends back ({@code -X}),
 * up ({@code +Y}) and right ({@code +Z}) from it. Expressed in the "left-front-bottom corner is the
 * origin, x runs back-to-front, y bottom-to-top, z left-to-right" convention, that puts the head at
 * {@code (4, 0, 2)} — 4 of the 5 cells back-to-front, on the bottom layer, in the middle column.
 *
 * <p><b>Clearance, and what it costs.</b> The head only keeps its front ({@code +X}) and top
 * ({@code +Y}) neighbours free; its back, left and right faces are walled in by the bottom plate's
 * casing ring. Automation therefore has two faces to attach to, not five — the price of sinking the
 * controller into the plate instead of floating it above the rig.
 *
 * <p><b>The drill contract.</b> The two structure blocks sit one column back from the head, at the
 * centre of the bottom plate {@code (-2,0,0)} and the centre of the top plate {@code (-2,4,0)}.
 * They are the plate centres rather than the head's own column, because the head now shares that
 * column with nothing: it is a corner block. Anything that needs to keep pointing at the structure
 * must follow the head's facing, which is independent of these offsets.
 *
 * <p>The layout is deliberately tier-independent: every block is one of the shared frame parts, so
 * the four {@code Set} fields below are the single source of truth for the shape, the material plan
 * and the projection overlay.
 */
public class StructureMinerMultiblock {
    /**
     * Each flange plate is a 5x5 perimeter ring plus the four spokes that reach the waist columns.
     * Without the spokes the plates would only touch air: the waist sits at radius 1, the ring at
     * radius 2, and nothing in between.
     *
     * <p>The bottom plate is the one that loses a block: its front-left corner cell is where the
     * controller stands, so that coordinate is deliberately absent here — keeping it would make the
     * build button try to stack casing on top of the head.
     */
    private static final Set<String> CASING =
            Set.of(
                    "-4,0,-2", "-4,0,-1", "-4,0,0", "-4,0,1", "-4,0,2", "-3,0,-2", "-3,0,0",
                    "-3,0,2", "-2,0,-2", "-2,0,-1", "-2,0,1", "-2,0,2", "-1,0,-2", "-1,0,0",
                    "-1,0,2", "0,0,-2", "0,0,-1", "0,0,1", "0,0,2", "-4,4,-2", "-4,4,-1", "-4,4,0",
                    "-4,4,1", "-4,4,2", "-3,4,-2", "-3,4,0", "-3,4,2", "-2,4,-2", "-2,4,-1",
                    "-2,4,1", "-2,4,2", "-1,4,-2", "-1,4,0", "-1,4,2", "0,4,-2", "0,4,-1", "0,4,0",
                    "0,4,1", "0,4,2");

    /**
     * The four corner posts of the waist (y = 1, 2, 3). Glass replaces the eight focus blocks the
     * old layout needed, and it runs the full height of the bay section, so each corner reads as a
     * window strip between the two flanges.
     */
    private static final Set<String> GLASS =
            Set.of(
                    "-3,1,-1", "-3,1,1", "-1,1,-1", "-1,1,1", "-3,2,-1", "-3,2,1", "-1,2,-1",
                    "-1,2,1", "-3,3,-1", "-3,3,1", "-1,3,-1", "-1,3,1");

    /**
     * The two structure blocks: the hard contract at the centre of the bottom plate, plus the drill
     * point at the centre of the top plate. Both are plate centres, and both keep the middle column
     * ({@code z = 0}) that the waist bays also use.
     */
    private static final Set<String> STRUCTURE = Set.of("-2,0,0", "-2,4,0");

    /**
     * The twelve module bays: four face-centred columns of three stacked slots (y = 1, 2, 3), each
     * seated on a flange spoke below and capped by a flange spoke above. A slot accepts a miner
     * upgrade block or the structure block, and the build button fills empty ones with the
     * structure block, so a fresh player can complete the multiblock out of the shared structure
     * recipe alone without needing any tier-gated machine product.
     */
    private static final Set<String> UPGRADE =
            Set.of(
                    "-2,1,-1", "-2,1,1", "-3,1,0", "-1,1,0", "-2,2,-1", "-2,2,1", "-3,2,0",
                    "-1,2,0", "-2,3,-1", "-2,3,1", "-3,3,0", "-1,3,0");

    private StructureMinerMultiblock() {}

    /*
     * Geometry invariants, asserted once at class-init. A pattern coordinate is a bare string, so a
     * typo would otherwise only show up as a machine that silently refuses to assemble; these fail
     * loudly at class load instead. They encode exactly the contracts the head-placement change
     * depends on:
     *
     *   1. no two of the four sets claim the same cell;
     *   2. every block stays inside the 5x5x5 body that grows backwards, upwards and rightwards
     *      from the head at the front-left corner;
     *   3. the head's own cell is never claimed by a pattern block, so placement can never stack
     *      casing on the controller.
     *
     * The old source tree carried a docs/tools/multiblock_geometry_check.py for this; the script
     * never made the port, so the invariants live here where they cannot be lost again.
     */
    static {
        Set<BlockPos> claimed =
                new HashSet<>(CASING.size() + GLASS.size() + STRUCTURE.size() + UPGRADE.size());
        for (Set<String> coordinates : List.of(CASING, GLASS, STRUCTURE, UPGRADE)) {
            for (String coordinate : coordinates) {
                int[] components = parse(coordinate);
                BlockPos offset = new BlockPos(components[0], components[1], components[2]);
                if (!claimed.add(offset)) {
                    throw new IllegalStateException("Overlapping multiblock offset " + offset);
                }
                if (!inBody(offset)) {
                    throw new IllegalStateException(
                            "Multiblock block " + offset + " escapes the 5x5x5 body");
                }
            }
        }
        if (claimed.contains(BlockPos.ZERO)) {
            throw new IllegalStateException(
                    "A pattern block claims the controller's own cell at the origin");
        }
    }

    /**
     * Whether a head-relative offset lands inside the 5x5x5 body box. The box grows backwards,
     * upwards and rightwards from the head, which occupies the box's front-left-bottom corner, so
     * every axis span is {@code [HEAD, HEAD + 4]} with the head at the origin.
     */
    private static boolean inBody(BlockPos offset) {
        int extent = 4;
        return offset.getX() >= -extent
                && offset.getX() <= 0
                && offset.getY() >= 0
                && offset.getY() <= extent
                && offset.getZ() >= -2
                && offset.getZ() <= extent - 2;
    }

    public enum ProjectionKind {
        CASING,
        GLASS,
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

    public static List<ProjectionBlock> projection() {
        BlockState casing = ModBlocks.STRUCTURE_MINER_CASING.get().defaultBlockState();
        BlockState glass = ModBlocks.STRUCTURE_MINER_GLASS.get().defaultBlockState();
        BlockState structure = ModBlocks.STRUCTURE_MINER_STRUCTURE.get().defaultBlockState();
        // Sized off the sets themselves so a geometry change cannot leave the hint stale.
        List<ProjectionBlock> blocks =
                new ArrayList<>(CASING.size() + GLASS.size() + STRUCTURE.size() + UPGRADE.size());
        addProjection(blocks, CASING, casing, ProjectionKind.CASING);
        addProjection(blocks, GLASS, glass, ProjectionKind.GLASS);
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
            int[] components = parse(coordinate);
            blocks.add(
                    new ProjectionBlock(
                            new BlockPos(components[0], components[1], components[2]),
                            state,
                            kind));
        }
    }

    /**
     * How many blocks of each kind the pattern needs, indexed by {@link ProjectionKind#ordinal()}.
     * Read straight off the coordinate sets, so the shift-tooltip material list can never drift
     * from the geometry.
     */
    public static int[] projectionCounts() {
        int[] counts = new int[ProjectionKind.values().length];
        counts[ProjectionKind.CASING.ordinal()] = CASING.size();
        counts[ProjectionKind.GLASS.ordinal()] = GLASS.size();
        counts[ProjectionKind.STRUCTURE.ordinal()] = STRUCTURE.size();
        counts[ProjectionKind.UPGRADE.ordinal()] = UPGRADE.size();
        return counts;
    }

    /** Splits one {@code "x,y,z"} key into its three ints. */
    private static int[] parse(String coordinate) {
        String[] components = coordinate.split(",");
        return new int[] {
            Integer.parseInt(components[0]),
            Integer.parseInt(components[1]),
            Integer.parseInt(components[2])
        };
    }

    public static List<StructureMinerUpgradeBlock> upgrades(ServerLevel level, BlockPos center) {
        List<StructureMinerUpgradeBlock> blocks = new ArrayList<>(UPGRADE.size());
        for (String coordinate : UPGRADE) {
            int[] offset = parse(coordinate);
            if (level.getBlockState(center.offset(offset[0], offset[1], offset[2])).getBlock()
                    instanceof StructureMinerUpgradeBlock upgrade) {
                blocks.add(upgrade);
            }
        }
        return List.copyOf(blocks);
    }

    /** Read-only material plan for the build button; never mutates the level. */
    public static BuildPlan planMaterials(ServerLevel level, BlockPos center) {
        Map<Block, Integer> required = new LinkedHashMap<>();
        List<BlockPos> blocked = new ArrayList<>();
        for (ProjectionBlock projected : projection()) {
            BlockPos target = center.offset(projected.offset());
            BlockState actual = level.getBlockState(target);
            if (isFilled(projected, actual)) continue;
            if (!actual.isAir() && !actual.canBeReplaced()) {
                blocked.add(target);
                continue;
            }
            required.merge(projected.state().getBlock(), 1, Integer::sum);
        }
        return new BuildPlan(Map.copyOf(required), List.copyOf(blocked));
    }

    /** Whether the given block state may occupy the upgrade slots of the multiblock. */
    public static boolean acceptsUpgradeSlot(BlockState state) {
        return state.getBlock() instanceof StructureMinerUpgradeBlock
                || state.is(ModBlocks.STRUCTURE_MINER_STRUCTURE.get());
    }

    /**
     * Whether {@code actual} already satisfies the slot {@code projected} points at. The bays are
     * modelled from the structure block but accept any upgrade block, so they cannot be compared
     * block-for-block; every other slot has to match its projected block exactly.
     *
     * <p>This is the only place that rule lives: the planner, the completeness test and the
     * client-side overlay all ask it, so a bay full of upgrades can never read as "missing" to one
     * of them and "filled" to another.
     */
    public static boolean isFilled(ProjectionBlock projected, BlockState actual) {
        return projected.kind() == ProjectionKind.UPGRADE
                ? acceptsUpgradeSlot(actual)
                : actual.is(projected.state().getBlock());
    }

    /**
     * The one completeness test, shared by the block entity and the client-side projection: it only
     * reads block states, so both a {@link ServerLevel} and a {@code ClientLevel} can call it.
     * Walks the projection rather than the bounding box, so it asks {@link #isFilled} about the
     * same slots the planner and the overlay see, in the same order.
     */
    public static boolean isComplete(LevelReader level, BlockPos center) {
        for (ProjectionBlock projected : projection()) {
            if (!isFilled(projected, level.getBlockState(center.offset(projected.offset())))) {
                return false;
            }
        }
        return true;
    }

    public static void place(ServerLevel level, BlockPos center) {
        for (ProjectionBlock projected : projection()) {
            level.setBlock(center.offset(projected.offset()), projected.state(), Block.UPDATE_ALL);
        }
    }

    /**
     * Builds the full multiblock, charging the player for whatever it places. Nothing is mutated
     * until the whole plan validates, so an obstructed or understocked attempt leaves the world
     * untouched rather than producing a half-built structure. Positions that already hold the right
     * block are not charged again, which makes a repeated press free.
     *
     * @return a message to show the player, or {@code null} when the build succeeded or had nothing
     *     left to place
     */
    public static Component buildFromInventory(ServerLevel level, BlockPos center, Player player) {
        BuildPlan plan = planMaterials(level, center);
        if (!plan.isClear()) {
            return Component.translatable(
                    "message.dimension_tech.structure_miner.build_blocked", plan.blocked().size());
        }
        if (plan.isSatisfied()) return null;

        // Creative players place for free; only survival pays out of the inventory.
        if (!player.getAbilities().instabuild) {
            Map<Block, Integer> shortfall = missingFromInventory(player, plan.required());
            if (!shortfall.isEmpty()) {
                return shortfallMessage(shortfall);
            }
            consumeFromInventory(player, plan.required());
        }
        place(level, center);
        return null;
    }

    /** Compares a plan against the player's carried blocks; empty when they can pay in full. */
    private static Map<Block, Integer> missingFromInventory(
            Player player, Map<Block, Integer> required) {
        Map<Block, Integer> shortfall = new LinkedHashMap<>();
        for (Map.Entry<Block, Integer> entry : required.entrySet()) {
            int available = countInInventory(player, entry.getKey());
            if (available < entry.getValue()) {
                shortfall.put(entry.getKey(), entry.getValue() - available);
            }
        }
        return shortfall;
    }

    private static int countInInventory(Player player, Block block) {
        Item item = block.asItem();
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void consumeFromInventory(Player player, Map<Block, Integer> required) {
        Inventory inventory = player.getInventory();
        for (Map.Entry<Block, Integer> entry : required.entrySet()) {
            Item item = entry.getKey().asItem();
            int remaining = entry.getValue();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.is(item)) continue;
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
            }
        }
        inventory.setChanged();
    }

    private static Component shortfallMessage(Map<Block, Integer> shortfall) {
        MutableComponent entries = Component.empty();
        boolean first = true;
        for (Map.Entry<Block, Integer> entry : shortfall.entrySet()) {
            if (!first) entries.append(Component.literal(", "));
            first = false;
            entries.append(
                    Component.translatable(
                            "message.dimension_tech.structure_miner.build_missing_entry",
                            entry.getKey().getName(),
                            entry.getValue()));
        }
        return Component.translatable(
                "message.dimension_tech.structure_miner.build_missing", entries);
    }
}
