package com.suntide_20210418.dimensiontech.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Side-agnostic handle for the item output chamber's optional ME grid node.
 *
 * <p><b>No AE2 type appears here on purpose.</b> The item output chamber's block entity is always
 * loaded — it is registered unconditionally and constructed whenever the block is — so it must
 * never name an AE2 class, or the mod would fail class verification on a client without AE2. The
 * concrete implementation lives in {@code integration.ae2} and is only resolved when AE2 is
 * present; see {@link MachineGridNodes}.
 */
public interface MachineGridNode {
    /** Joins the grid at this position; call from {@code onLoad} on the server. */
    void create(Level level, BlockPos position);

    /** Restores the node's saved data; the node itself is created separately. */
    void load(CompoundTag tag);

    /** Writes the node's data. */
    void save(CompoundTag tag);

    /** Leaves the grid; call from {@code setRemoved}. */
    void destroy();

    /** Whether the node is joined to a powered, booted grid. */
    boolean isOnline();

    /** Offers a stack to the network, returning the part the network did not take. */
    ItemStack insert(ItemStack stack);
}
