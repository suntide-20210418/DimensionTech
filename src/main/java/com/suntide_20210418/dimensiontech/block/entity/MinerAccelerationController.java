package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.mythicminer.processing.ExternalTickAcceleration;
import com.suntide_20210418.dimensiontech.loot.expectation.ExpectationMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Owns accelerated slot advancement and the shared 400-natural-tick completion window. */
final class MinerAccelerationController {
    static final int MINIMUM_NATURAL_TICKS = ExternalTickAcceleration.MINIMUM_NATURAL_TICKS;
    private final MythicMinerSlotProgress progress;
    private final int[] processingTimes;
    private final int[] parallelHundredths;
    private final int[] parallelFractionHundredths;
    private final int[] quantityFractionHundredths;
    private final ExternalTickAcceleration[] acceleration;

    MinerAccelerationController(int slotCount) {
        this.progress = new MythicMinerSlotProgress(slotCount);
        this.processingTimes = new int[slotCount];
        this.parallelHundredths = new int[slotCount];
        this.parallelFractionHundredths = new int[slotCount];
        this.quantityFractionHundredths = new int[slotCount];
        this.acceleration = new ExternalTickAcceleration[slotCount];
        for (int slot = 0; slot < slotCount; slot++) {
            this.acceleration[slot] = new ExternalTickAcceleration();
        }
    }

    int slotCount() {
        return processingTimes.length;
    }

    int progress(int slot) {
        return valid(slot) ? progress.get(slot) : 0;
    }

    int processingTime(int slot) {
        return valid(slot) ? processingTimes[slot] : 0;
    }

    int currentParallelHundredths(int slot) {
        return valid(slot) ? parallelHundredths[slot] : 0;
    }

    int parallelFraction(int slot) {
        return valid(slot) ? parallelFractionHundredths[slot] : 0;
    }

    int quantityFraction(int slot) {
        return valid(slot) ? quantityFractionHundredths[slot] : 0;
    }

    long currentActualTicks(int slot) {
        return valid(slot) ? acceleration[slot].currentActualTicks() : 0L;
    }

    long currentNaturalTicks(int slot) {
        return valid(slot) ? acceleration[slot].currentNaturalTicks() : 0L;
    }

    long previousActualTicks(int slot) {
        return valid(slot) ? acceleration[slot].previousActualTicks() : 0L;
    }

    int previousExtraParallel(int slot) {
        return valid(slot) ? acceleration[slot].previousExtraParallelHundredths() : 0;
    }

    int currentExtraParallel(int slot) {
        return valid(slot) ? acceleration[slot].currentExtraParallelHundredths() : 0;
    }

    long equivalentTicks(int slot) {
        return valid(slot) ? acceleration[slot].currentEquivalentAccelerationTicks() : 0L;
    }

    double cycleEquivalent(int slot) {
        return valid(slot) ? acceleration[slot].currentCycleEquivalentAcceleration() : 0.0D;
    }

    boolean waitingForNaturalWindow(int slot) {
        return valid(slot) && acceleration[slot].waitingForNaturalWindow();
    }

    void setPlan(int slot, int processingTime, int parallel) {
        if (!valid(slot)) return;
        processingTimes[slot] = Math.max(0, processingTime);
        parallelHundredths[slot] = Math.max(0, parallel);
        progress.clamp(slot, processingTimes[slot]);
    }

    void clearPlans() {
        Arrays.fill(processingTimes, 0);
        Arrays.fill(parallelHundredths, 0);
    }

    int drawParallel(int slot, long averageParallelHundredths) {
        if (!valid(slot)) return 0;
        long whole = parallelFractionHundredths[slot] + Math.max(0L, averageParallelHundredths);
        int result = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, whole / 100L));
        parallelFractionHundredths[slot] = (int) (whole % 100L);
        return result;
    }

    int drawsForQuantity(int slot, long scaled) {
        if (!valid(slot)) return 0;
        long whole = quantityFractionHundredths[slot] + Math.max(0L, scaled);
        int result = (int) Math.min(Integer.MAX_VALUE, whole / 100L);
        quantityFractionHundredths[slot] = (int) (whole % 100L);
        return result;
    }

    int drawsForQuantity(int slot, int parallel, double quantity, double quantityReference) {
        int factor = ExpectationMath.quantityFactorHundredths(quantity, quantityReference);
        long scaled = (long) parallel * 8L * factor;
        return drawsForQuantity(slot, scaled);
    }

    int parallelFraction() {
        for (int slot = 0; slot < processingTimes.length; slot++)
            if (processingTimes[slot] > 0) return parallelFraction(slot);
        return 0;
    }

    int[] progressValues() {
        return progress.save();
    }

    void save(
            CompoundTag tag,
            String progressTag,
            String parallelFractionTag,
            String quantityFractionTag,
            String accelerationTagName) {
        tag.putIntArray(progressTag, progress.save());
        tag.putIntArray(parallelFractionTag, parallelFractionHundredths);
        tag.putIntArray(quantityFractionTag, quantityFractionHundredths);
        ListTag states = new ListTag();
        for (ExternalTickAcceleration value : acceleration) {
            var state = value.save();
            CompoundTag entry = new CompoundTag();
            entry.putLong("LastGameTime", state.lastGameTime());
            entry.putLong("ActualTicks", state.actualTicks());
            entry.putLong("NaturalTicks", state.naturalTicks());
            entry.putLong("ActualTicksAtNaturalTickStart", state.actualTicksAtNaturalTickStart());
            entry.putLong("EquivalentAccelerationTicks", state.equivalentAccelerationTicks());
            entry.putBoolean("TargetReached", state.targetReached());
            entry.putInt("SettledExtraParallelHundredths", state.settledExtraParallelHundredths());
            entry.putLong("PreviousActualTicks", state.previousActualTicks());
            entry.putInt(
                    "PreviousExtraParallelHundredths", state.previousExtraParallelHundredths());
            entry.putBoolean("ExternalParallelEligible", state.externalParallelEligible());
            states.add(entry);
        }
        tag.put(accelerationTagName, states);
    }

    void load(
            CompoundTag tag,
            String progressTag,
            String legacyProgressTag,
            String parallelFractionTag,
            String legacyParallelFractionTag,
            String quantityFractionTag,
            String legacyQuantityFractionTag,
            String accelerationTagName) {
        if (tag.contains(progressTag, Tag.TAG_INT_ARRAY))
            progress.load(tag.getIntArray(progressTag));
        else progress.load(new int[] {Math.max(0, tag.getInt(legacyProgressTag))});
        loadFractions(
                parallelFractionHundredths,
                tag.contains(parallelFractionTag, Tag.TAG_INT_ARRAY)
                        ? tag.getIntArray(parallelFractionTag)
                        : new int[] {tag.getInt(legacyParallelFractionTag)});
        loadFractions(
                quantityFractionHundredths,
                tag.contains(quantityFractionTag, Tag.TAG_INT_ARRAY)
                        ? tag.getIntArray(quantityFractionTag)
                        : new int[] {tag.getInt(legacyQuantityFractionTag)});
        if (!tag.contains(accelerationTagName, Tag.TAG_LIST)) return;
        ListTag states = tag.getList(accelerationTagName, Tag.TAG_COMPOUND);
        for (int slot = 0; slot < Math.min(states.size(), acceleration.length); slot++) {
            CompoundTag e = states.getCompound(slot);
            long actualTicks = readLongCompat(e, "ActualTicks", "StatisticsActualTicks");
            long naturalTicks = readLongCompat(e, "NaturalTicks", "StatisticsNaturalTicks");
            boolean targetReached =
                    e.contains("TargetReached", Tag.TAG_BYTE)
                            ? e.getBoolean("TargetReached")
                            : actualTicks
                                            >= ExternalTickAcceleration
                                                    .MINIMUM_NATURAL_TICKS
                                    && naturalTicks
                                            < ExternalTickAcceleration
                                                    .MINIMUM_NATURAL_TICKS;
            acceleration[slot].load(
                    new ExternalTickAcceleration.State(
                            e.getLong("LastGameTime"),
                            actualTicks,
                            naturalTicks,
                            readLongCompat(e, "ActualTicksAtNaturalTickStart", null),
                            readLongCompat(e, "EquivalentAccelerationTicks", null),
                            targetReached,
                            Math.max(0, e.getInt("SettledExtraParallelHundredths")),
                            readLongCompat(e, "PreviousActualTicks", null),
                            Math.max(0, e.getInt("PreviousExtraParallelHundredths")),
                            !e.contains("ExternalParallelEligible", Tag.TAG_BYTE)
                                    || e.getBoolean("ExternalParallelEligible")));
        }
    }

    private static long readLongCompat(CompoundTag tag, String key, String legacyKey) {
        if (tag.contains(key, Tag.TAG_LONG)) return Math.max(0L, tag.getLong(key));
        if (tag.contains(key, Tag.TAG_INT)) return Math.max(0L, tag.getInt(key));
        if (legacyKey != null && tag.contains(legacyKey, Tag.TAG_LONG))
            return Math.max(0L, tag.getLong(legacyKey));
        if (legacyKey != null && tag.contains(legacyKey, Tag.TAG_INT))
            return Math.max(0L, tag.getInt(legacyKey));
        return 0L;
    }

    private static void loadFractions(int[] target, int[] saved) {
        Arrays.fill(target, 0);
        for (int i = 0; i < Math.min(target.length, saved.length); i++)
            target[i] = Math.max(0, Math.min(99, saved[i]));
    }

    private boolean valid(int slot) {
        return slot >= 0 && slot < processingTimes.length;
    }

    List<CompletedSlot> advance(long gameTime, boolean[] enabledSlots) {
        List<CompletedSlot> completed = new ArrayList<>();
        for (int slot = 0; slot < processingTimes.length; slot++) {
            if (!enabledSlots[slot] || processingTimes[slot] <= 0) continue;
            ExternalTickAcceleration.Observation observation =
                    acceleration[slot].observe(gameTime, processingTimes[slot]);
            progress.set(
                    slot, (int) Math.min(Integer.MAX_VALUE, observation.logicalProgressTicks()));
            if (observation.complete()) {
                completed.add(new CompletedSlot(slot, observation.completedParallel()));
            }
        }
        return List.copyOf(completed);
    }

    void reset(int slot) {
        if (!valid(slot)) return;
        progress.reset(slot);
        processingTimes[slot] = 0;
        parallelHundredths[slot] = 0;
        parallelFractionHundredths[slot] = 0;
        quantityFractionHundredths[slot] = 0;
        acceleration[slot] = new ExternalTickAcceleration();
    }

    record CompletedSlot(int slot, int parallel) {}
}
