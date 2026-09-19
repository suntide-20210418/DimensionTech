package com.suntide_20210418.dimensiontech.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Reusable energy contract for block entities.
 *
 * <p>The Forge capability methods describe external transfer. {@link #consume(int)} is the separate
 * machine-side operation used when a block entity pays an internal cost.
 */
public interface EnergyContainer extends IEnergyStorage {
    /** Returns whether the complete amount can be paid without changing the stored energy. */
    boolean canConsume(int amount);

    /** Pays an internal cost atomically, returning false when the balance is insufficient. */
    boolean consume(int amount);

    /** Changes the stored energy and clamps it to the current capacity. */
    void setEnergy(int energy);

    /** Changes capacity and clamps stored energy when the capacity is reduced. */
    void setCapacity(int capacity);

    /** Encodes this container under the supplied key. */
    void save(CompoundTag tag, String key);

    /** Restores this container from the supplied key, ignoring a missing or invalid value. */
    void load(CompoundTag tag, String key);
}
