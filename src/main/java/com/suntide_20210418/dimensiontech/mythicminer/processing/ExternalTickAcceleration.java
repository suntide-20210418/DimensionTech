package com.suntide_20210418.dimensiontech.mythicminer.processing;

/** Tracks one marker slot's natural progress and accelerated execution count. */
public final class ExternalTickAcceleration {
    public static final int MINIMUM_NATURAL_TICKS = 400;
    private static final long UNSET_GAME_TIME = Long.MIN_VALUE;

    private long lastGameTime = UNSET_GAME_TIME;
    private long actualTicks;
    private long naturalTicks;
    private long actualTicksAtNaturalTickStart;
    private long expectedCallsInNaturalTick = 1L;
    private long callsInNaturalTick;
    private long equivalentAccelerationTicks;
    private boolean targetReached;
    private long currentCycleTicks;
    private int settledExtraParallelHundredths;
    private long previousActualTicks;
    private int previousExtraParallelHundredths;
    private boolean externalParallelEligible;

    public Observation observe(long gameTime, int cycleTicks) {
        boolean newNaturalTick = gameTime != lastGameTime;
        externalParallelEligible = cycleTicks > 0;
        long effectiveCycleTicks = Math.max(MINIMUM_NATURAL_TICKS, (long) cycleTicks);
        currentCycleTicks = effectiveCycleTicks;
        if (newNaturalTick) {
            // The external ticker invokes this machine once for roughly every two
            // accelerated ticks. Restore the skipped half while retaining the ordinary
            // server tick as the baseline: 2, 3, 5 calls become 2, 4, 8 ticks.
            long callsSinceNaturalTick = actualTicks - actualTicksAtNaturalTickStart;
            long correctedCalls = correctedNaturalInterval(callsSinceNaturalTick);
            equivalentAccelerationTicks = Math.max(0L, callsSinceNaturalTick - 1L);
            actualTicks = saturatedAdd(actualTicks, correctedCalls - callsSinceNaturalTick);
            actualTicksAtNaturalTickStart = actualTicks;
            expectedCallsInNaturalTick = Math.max(1L, callsSinceNaturalTick);
            callsInNaturalTick = 0L;
            lastGameTime = gameTime;
            naturalTicks = saturatedIncrement(naturalTicks);
        }
        actualTicks = saturatedIncrement(actualTicks);
        callsInNaturalTick = saturatedIncrement(callsInNaturalTick);

        boolean reachedCycleTicks = actualTicks >= effectiveCycleTicks;
        if (reachedCycleTicks && naturalTicks < MINIMUM_NATURAL_TICKS) {
            targetReached = true;
        }

        boolean complete = false;
        int completedParallel = 0;
        long completedActualTicks = 0L;
        boolean naturalWindowSettled = callsInNaturalTick >= expectedCallsInNaturalTick;
        if (targetReached && naturalTicks >= MINIMUM_NATURAL_TICKS && naturalWindowSettled) {
            actualTicks = settleCurrentNaturalInterval(actualTicks, callsInNaturalTick);
            complete = true;
            completedParallel =
                    externalParallelEligible
                            ? extraParallelForRatio(actualTicks, effectiveCycleTicks)
                            : 0;
        } else if (reachedCycleTicks
                && naturalTicks >= MINIMUM_NATURAL_TICKS
                && naturalWindowSettled) {
            actualTicks = settleCurrentNaturalInterval(actualTicks, callsInNaturalTick);
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
            expectedCallsInNaturalTick = 1L;
            callsInNaturalTick = 0L;
            targetReached = false;
        }

        return new Observation(
                naturalTicks, actualTicks, complete, completedParallel, completedActualTicks);
    }

    public long currentActualTicks() {
        return actualTicks;
    }

    public long currentNaturalTicks() {
        return naturalTicks;
    }

    public long currentCycleNaturalTicks() {
        return naturalTicks;
    }

    public boolean waitingForNaturalWindow() {
        return targetReached && naturalTicks < MINIMUM_NATURAL_TICKS;
    }

    public long currentEquivalentAccelerationTicks() {
        return equivalentAccelerationTicks;
    }

    public double currentCycleEquivalentAcceleration() {
        return naturalTicks <= 0L ? 0.0D : actualTicks / (double) naturalTicks;
    }

    public int settledExtraParallelHundredths() {
        return settledExtraParallelHundredths;
    }

    public int currentExtraParallelHundredths() {
        return targetReached
                        && externalParallelEligible
                        && naturalTicks > 0L
                        && naturalTicks < MINIMUM_NATURAL_TICKS
                ? extraParallelForRatio(actualTicks, currentCycleTicks)
                : 0;
    }

    public long previousActualTicks() {
        return previousActualTicks;
    }

    public int previousExtraParallelHundredths() {
        return previousExtraParallelHundredths;
    }

    public State save() {
        return new State(
                lastGameTime,
                actualTicks,
                naturalTicks,
                actualTicksAtNaturalTickStart,
                equivalentAccelerationTicks,
                targetReached,
                settledExtraParallelHundredths,
                previousActualTicks,
                previousExtraParallelHundredths,
                externalParallelEligible);
    }

    public void load(State state) {
        lastGameTime = state.lastGameTime();
        actualTicks = Math.max(0L, state.actualTicks());
        naturalTicks = Math.max(0L, state.naturalTicks());
        actualTicksAtNaturalTickStart = Math.max(0L, state.actualTicksAtNaturalTickStart());
        expectedCallsInNaturalTick = 1L;
        callsInNaturalTick = 0L;
        equivalentAccelerationTicks = Math.max(0L, state.equivalentAccelerationTicks());
        targetReached = state.targetReached();
        settledExtraParallelHundredths = Math.max(0, state.settledExtraParallelHundredths());
        previousActualTicks = Math.max(0L, state.previousActualTicks());
        previousExtraParallelHundredths = Math.max(0, state.previousExtraParallelHundredths());
        externalParallelEligible = state.externalParallelEligible();
    }

    private static int extraParallelForRatio(long actualTicks, long naturalTicks) {
        if (actualTicks <= 0L || naturalTicks <= 0L) return 0;
        double extraParallel =
                Math.sqrt(Math.max(0.0D, actualTicks / (double) naturalTicks - 1.0D));
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(extraParallel * 100.0D));
    }

    private static long saturatedIncrement(long value) {
        if (value < 0L) return 0L;
        return value == Long.MAX_VALUE ? value : value + 1L;
    }

    private static long correctedNaturalInterval(long calls) {
        if (calls <= 0L) return 0L;
        if (calls == 1L) return 1L;
        if (calls > 129L) return calls;
        return calls > Long.MAX_VALUE / 2L + 1L ? Long.MAX_VALUE : calls * 2L - 2L;
    }

    private static long settleCurrentNaturalInterval(long actualTicks, long calls) {
        long corrected = correctedNaturalInterval(calls);
        return saturatedAdd(actualTicks, corrected - calls);
    }

    private static long saturatedAdd(long left, long right) {
        if (right <= 0L) return left;
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public record Observation(
            long logicalProgressTicks,
            long actualProgressTicks,
            boolean complete,
            int completedParallel,
            long completedActualTicks) {}

    public record State(
            long lastGameTime,
            long actualTicks,
            long naturalTicks,
            long actualTicksAtNaturalTickStart,
            long equivalentAccelerationTicks,
            boolean targetReached,
            int settledExtraParallelHundredths,
            long previousActualTicks,
            int previousExtraParallelHundredths,
            boolean externalParallelEligible) {}
}
