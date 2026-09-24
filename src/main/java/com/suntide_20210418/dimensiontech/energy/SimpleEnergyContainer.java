package com.suntide_20210418.dimensiontech.energy;

import java.util.Objects;
import java.util.function.IntConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Default receive-only energy container with an optional state-change callback. */
public final class SimpleEnergyContainer implements EnergyContainer {
    private final boolean canReceive;
    private final boolean canExtract;
    private final IntConsumer changeListener;
    private int capacity;
    private int energy;

    public SimpleEnergyContainer(int capacity) {
        this(capacity, true, false, ignored -> {});
    }

    public SimpleEnergyContainer(
            int capacity, boolean canReceive, boolean canExtract, IntConsumer changeListener) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Energy capacity must be positive");
        }
        this.capacity = capacity;
        this.canReceive = canReceive;
        this.canExtract = canExtract;
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive || maxReceive <= 0) return 0;
        int received = Math.min(capacity - energy, maxReceive);
        if (!simulate && received > 0) {
            energy += received;
            changed();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract || maxExtract <= 0) return 0;
        int extracted = Math.min(energy, maxExtract);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            changed();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return canExtract;
    }

    @Override
    public boolean canReceive() {
        return canReceive;
    }

    @Override
    public boolean canConsume(int amount) {
        return amount > 0 && energy >= amount;
    }

    @Override
    public boolean consume(int amount) {
        if (!canConsume(amount)) return false;
        energy -= amount;
        changed();
        return true;
    }

    @Override
    public void setEnergy(int energy) {
        this.energy = Math.min(capacity, Math.max(0, energy));
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
        energy = Math.min(energy, this.capacity);
    }

    @Override
    public void save(CompoundTag tag, String key) {
        tag.putInt(key, energy);
    }

    @Override
    public void load(CompoundTag tag, String key) {
        if (tag.contains(key, Tag.TAG_INT)) {
            setEnergy(tag.getInt(key));
        }
    }

    private void changed() {
        changeListener.accept(energy);
    }
}
