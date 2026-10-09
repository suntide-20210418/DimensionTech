package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.integration.MachineGridNode;
import com.suntide_20210418.dimensiontech.integration.MachineGridNodes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The miner's output routing, carried by the item output chamber.
 *
 * <p><b>One group per attempt.</b> The chamber ejects the head of the queue into its own adjacent
 * containers and hands the remainder back, so a large batch leaves a stack at a time rather than
 * all at once; the miner keeps the queue and calls {@link #eject} again on the next tick.
 *
 * <p><b>ME device when AE2 is present.</b> The chamber registers a grid node from its own position,
 * so cables can reach it and it shows up on the network like any other device. While that node is
 * online, an ejection attempt pushes the whole queue into network storage at once instead of one
 * group at a time — the second behaviour in the spec, and the reason the two paths are not merged.
 */
public final class ItemOutputChamberBlockEntity extends MinerChamberBlockEntity {
    private static final String GRID_NODE_TAG = "GridNode";

    /** Null when AE2 is absent, in which case the chamber has no grid identity at all. */
    private final MachineGridNode gridNode = MachineGridNodes.create(this);

    public ItemOutputChamberBlockEntity(BlockPos position, BlockState blockState) {
        super(ModBlockEntities.ITEM_OUTPUT_CHAMBER.get(), position, blockState);
    }

    public MachineGridNode gridNode() {
        return gridNode;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (gridNode != null && level != null && !level.isClientSide) {
            gridNode.create(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (gridNode != null) gridNode.destroy();
        super.setRemoved();
    }

    /** Nothing periodic: the miner calls {@link #eject} explicitly when the queue is not empty. */
    @Override
    public void chamberTick(BaseMinerBlockEntity miner) {}

    /**
     * Attempts one ejection and returns the stacks that stayed behind.
     *
     * <p>An online grid takes the whole queue in one pass; otherwise exactly one group leaves,
     * whether or not the adjacent containers had room for all of it.
     */
    public List<ItemStack> eject(ServerLevel level, List<ItemStack> pending) {
        if (pending.isEmpty()) return List.of();
        if (gridNode != null && gridNode.isOnline()) {
            List<ItemStack> remainder = new ArrayList<>(pending.size());
            for (ItemStack stack : pending) {
                ItemStack left = gridNode.insert(stack);
                if (!left.isEmpty()) remainder.add(left);
            }
            return List.copyOf(remainder);
        }
        return ejectOneGroup(level, pending);
    }

    /** Pushes the head of the queue into this chamber's adjacent containers, as far as it fits. */
    private List<ItemStack> ejectOneGroup(ServerLevel level, List<ItemStack> pending) {
        ItemStack remainder = pending.get(0);
        for (Direction direction : Direction.values()) {
            if (remainder.isEmpty()) break;
            IItemHandler handler =
                    level.getCapability(
                            Capabilities.ItemHandler.BLOCK,
                            worldPosition.relative(direction),
                            direction.getOpposite());
            if (handler == null) continue;
            remainder = ItemHandlerHelper.insertItemStacked(handler, remainder, false);
        }
        List<ItemStack> next = new ArrayList<>(pending.size());
        if (!remainder.isEmpty()) next.add(remainder);
        next.addAll(pending.subList(1, pending.size()));
        // Nothing moved, so the queue is returned untouched rather than rebuilt into an equal copy.
        return next.equals(pending) ? pending : List.copyOf(next);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (gridNode != null) {
            CompoundTag nodeTag = new CompoundTag();
            gridNode.save(nodeTag);
            tag.put(GRID_NODE_TAG, nodeTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (gridNode != null && tag.contains(GRID_NODE_TAG, Tag.TAG_COMPOUND)) {
            gridNode.load(tag.getCompound(GRID_NODE_TAG));
        }
    }
}
