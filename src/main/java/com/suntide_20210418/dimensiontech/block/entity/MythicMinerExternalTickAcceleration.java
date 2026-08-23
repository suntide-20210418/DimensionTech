package com.suntide_20210418.dimensiontech.block.entity;

/** Tracks one marker slot's natural progress and accelerated execution count. */
final class MythicMinerExternalTickAcceleration {
    static final int MINIMUM_NATURAL_TICKS = 400;
    private static final long UNSET_GAME_TIME = Long.MIN_VALUE;

    private long lastGameTime = UNSET_GAME_TIME;
    private long actualTicks;
    private long naturalTicks;
    private long actualTicksAtNaturalTickStart;
    private long equivalentAccelerationTicks;
    private boolean targetReached;
    private long currentCycleTicks;
    private int settledExtraParallelHundredths;
    private long previousActualTicks;
    private int previousExtraParallelHundredths;
    private boolean externalParallelEligible;

    Observation observe(long gameTime, int cycleTicks) {
        boolean newNaturalTick = gameTime != lastGameTime;
        externalParallelEligible = cycleTicks > 0;
        long effectiveCycleTicks = Math.max(MINIMUM_NATURAL_TICKS, (long) cycleTicks);
        currentCycleTicks = effectiveCycleTicks;
        if (newNaturalTick) {
            // Every natural tick already performs one ordinary serverTick call. The
            // equivalent external acceleration reports the additional accelerated calls,
            // so remove that baseline call while retaining a minimum of 1x.
            long callsSinceNaturalTick = actualTicks - actualTicksAtNaturalTickStart;
            equivalentAccelerationTicks = callsSinceNaturalTick <= 0L
                    ? 0L
                    : Math.max(1L, callsSinceNaturalTick - 1L);
            actualTicksAtNaturalTickStart = actualTicks;
            lastGameTime = gameTime;
            naturalTicks = saturatedIncrement(naturalTicks);
        }
        actualTicks = saturatedIncrement(actualTicks);

        boolean reachedCycleTicks = actualTicks >= effectiveCycleTicks;
        if (reachedCycleTicks && naturalTicks < MINIMUM_NATURAL_TICKS) {
            targetReached = true;
        }

        boolean complete = false;
        int completedParallel = 0;
        long completedActualTicks = 0L;
        if (targetReached && naturalTicks >= MINIMUM_NATURAL_TICKS) {
            complete = true;
            completedParallel = externalParallelEligible
                    ? extraParallelForRatio(actualTicks, effectiveCycleTicks)
                    : 0;
        } else if (reachedCycleTicks && naturalTicks >= MINIMUM_NATURAL_TICKS) {
            complete = true;
        }

        if (complete) {
            completedActualTicks = actualTicks;
            previousActualTicks = actualTicks;
            previousExtraParallelHundredths = completedParallel;
            settledExtraParallelHundredths = completedParallel;
            actualTicks = 0L;
            naturalTicks = 0L;
            actualTicksAtNaturalTickStart = 0L;
            targetReached = false;
        }

        return new Observation(
                naturalTicks, actualTicks, complete, completedParallel, completedActualTicks);
    }

    long currentActualTicks() { return actualTicks; }
    long currentNaturalTicks() { return naturalTicks; }
    long currentCycleNaturalTicks() { return naturalTicks; }
    long currentEquivalentAccelerationTicks() { return equivalentAccelerationTicks; }
    int settledExtraParallelHundredths() { return settledExtraParallelHundredths; }

    int currentExtraParallelHundredths() {
        return targetReached && externalParallelEligible && naturalTicks > 0L
                        && naturalTicks < MINIMUM_NATURAL_TICKS
                ? extraParallelForRatio(actualTicks, currentCycleTicks)
                : 0;
    }

    long previousActualTicks() { return previousActualTicks; }
    int previousExtraParallelHundredths() { return previousExtraParallelHundredths; }

    State save() {
        return new State(lastGameTime, actualTicks, naturalTicks, actualTicksAtNaturalTickStart,
                equivalentAccelerationTicks, targetReached,
                settledExtraParallelHundredths, previousActualTicks,
                previousExtraParallelHundredths, externalParallelEligible);
    }

    void load(State state) {
        lastGameTime = state.lastGameTime();
        actualTicks = Math.max(0L, state.actualTicks());
        naturalTicks = Math.max(0L, state.naturalTicks());
        actualTicksAtNaturalTickStart = Math.max(0L, state.actualTicksAtNaturalTickStart());
        equivalentAccelerationTicks = Math.max(0L, state.equivalentAccelerationTicks());
        targetReached = state.targetReached();
        settledExtraParallelHundredths = Math.max(0, state.settledExtraParallelHundredths());
        previousActualTicks = Math.max(0L, state.previousActualTicks());
        previousExtraParallelHundredths = Math.max(0, state.previousExtraParallelHundredths());
        externalParallelEligible = state.externalParallelEligible();
    }

    private static int extraParallelForRatio(long actualTicks, long naturalTicks) {
        if (actualTicks <= 0L || naturalTicks <= 0L) return 0;
        double extraParallel = Math.sqrt(Math.max(0.0D,
                actualTicks / (double) naturalTicks - 1.0D));
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(extraParallel * 100.0D));
    }

    private static long saturatedIncrement(long value) {
        if (value < 0L) return 0L;
        return value == Long.MAX_VALUE ? value : value + 1L;
    }

    record Observation(long logicalProgressTicks, long actualProgressTicks, boolean complete,
            int completedParallel, long completedActualTicks) {}

    record State(long lastGameTime, long actualTicks, long naturalTicks,
            long actualTicksAtNaturalTickStart, long equivalentAccelerationTicks,
            boolean targetReached,
            int settledExtraParallelHundredths, long previousActualTicks,
            int previousExtraParallelHundredths, boolean externalParallelEligible) {}
}
