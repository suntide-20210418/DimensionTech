package com.suntide_20210418.dimensiontech.block.entity;

import java.util.Arrays;

/** Independent cycle progress for each structure-marker slot. */
final class MythicMinerSlotProgress {
    private final int[] values;

    MythicMinerSlotProgress(int slotCount) {
        values = new int[slotCount];
    }

    boolean advance(int slot, int processingTime) {
        if (processingTime <= 0) {
            reset(slot);
            return false;
        }
        values[slot]++;
        if (values[slot] < processingTime) {
            return false;
        }
        values[slot] = 0;
        return true;
    }

    int get(int slot) {
        return values[slot];
    }

    void set(int slot, int value) {
        values[slot] = Math.max(0, value);
    }

    void reset(int slot) {
        values[slot] = 0;
    }

    void clamp(int slot, int processingTime) {
        values[slot] = Math.max(0, Math.min(values[slot], Math.max(0, processingTime - 1)));
    }

    int[] save() {
        return Arrays.copyOf(values, values.length);
    }

    void load(int[] saved) {
        Arrays.fill(values, 0);
        System.arraycopy(saved, 0, values, 0, Math.min(saved.length, values.length));
        for (int slot = 0; slot < values.length; slot++) {
            values[slot] = Math.max(0, values[slot]);
        }
    }
}
