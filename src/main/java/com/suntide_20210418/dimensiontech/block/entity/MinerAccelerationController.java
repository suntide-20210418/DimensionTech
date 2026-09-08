package com.suntide_20210418.dimensiontech.block.entity;

import java.util.ArrayList;
import java.util.List;

/** Owns accelerated slot advancement and the shared 400-natural-tick completion window. */
final class MinerAccelerationController {
    private final MythicMinerSlotProgress progress;
    private final int[] processingTimes;
    private final MythicMinerExternalTickAcceleration[] acceleration;

    MinerAccelerationController(
            MythicMinerSlotProgress progress,
            int[] processingTimes,
            MythicMinerExternalTickAcceleration[] acceleration) {
        this.progress = progress;
        this.processingTimes = processingTimes;
        this.acceleration = acceleration;
    }

    List<CompletedSlot> advance(long gameTime, boolean[] enabledSlots) {
        List<CompletedSlot> completed = new ArrayList<>();
        for (int slot = 0; slot < processingTimes.length; slot++) {
            if (!enabledSlots[slot] || processingTimes[slot] <= 0) continue;
            MythicMinerExternalTickAcceleration.Observation observation =
                    acceleration[slot].observe(gameTime, processingTimes[slot]);
            progress.set(slot, (int) Math.min(Integer.MAX_VALUE, observation.logicalProgressTicks()));
            if (observation.complete()) {
                completed.add(new CompletedSlot(slot, observation.completedParallel()));
            }
        }
        return List.copyOf(completed);
    }

    void reset(int slot) {
        progress.reset(slot);
        processingTimes[slot] = 0;
        acceleration[slot] = new MythicMinerExternalTickAcceleration();
    }

    record CompletedSlot(int slot, int parallel) {}
}
