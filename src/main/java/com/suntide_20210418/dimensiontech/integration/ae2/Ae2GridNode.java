package com.suntide_20210418.dimensiontech.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.integration.MachineGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The item output chamber's ME device identity.
 *
 * <p>AE2 discovers an in-world node host through the {@code AECapabilities.IN_WORLD_GRID_NODE_HOST}
 * block capability rather than an {@code instanceof} test — {@code GridHelper.getNodeHost} is a
 * {@code Level.getCapability} call — which is what makes this class optional: the chamber registers
 * this host as its capability only when AE2 is loaded, and the chamber itself never names an AE2
 * type.
 *
 * <p>{@code setInWorldNode(true)} is not optional. A managed node leaves that flag false by
 * default, and without it the node never takes part in in-world connections, so cables would find
 * the host and still not connect.
 */
public final class Ae2GridNode implements MachineGridNode, IInWorldGridNodeHost {
    private static final IActionSource ACTION_SOURCE = IActionSource.empty();

    private final IManagedGridNode node;

    private Ae2GridNode(BlockEntity owner) {
        this.node =
                GridHelper.createManagedNode(
                        owner,
                        (IGridNodeListener<BlockEntity>) (be, changedNode) -> be.setChanged());
        node.setInWorldNode(true);
        node.setVisualRepresentation(ModBlocks.ITEM_OUTPUT_CHAMBER.get());
    }

    public static MachineGridNode create(BlockEntity owner) {
        return new Ae2GridNode(owner);
    }

    @Override
    public IGridNode getGridNode(Direction direction) {
        return node.getNode();
    }

    @Override
    public void create(Level level, BlockPos position) {
        node.create(level, position);
    }

    @Override
    public void load(CompoundTag tag) {
        node.loadFromNBT(tag);
    }

    @Override
    public void save(CompoundTag tag) {
        node.saveToNBT(tag);
    }

    @Override
    public void destroy() {
        node.destroy();
    }

    @Override
    public boolean isOnline() {
        return node.isOnline();
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        var grid = node.getGrid();
        if (grid == null) return stack;
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) return stack;
        long inserted =
                grid.getStorageService()
                        .getInventory()
                        .insert(key, stack.getCount(), Actionable.MODULATE, ACTION_SOURCE);
        if (inserted <= 0) return stack;
        if (inserted >= stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink((int) inserted);
        return remainder;
    }
}
