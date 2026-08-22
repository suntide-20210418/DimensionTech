package com.suntide_20210418.dimensiontech.block.entity;

/** Tracks one marker slot's accelerated work cycle. */
final class MythicMinerExternalTickAcceleration {
    static final int MINIMUM_NATURAL_TICKS = 400;
    private static final long UNSET_GAME_TIME = Long.MIN_VALUE;

    private long lastGameTime = UNSET_GAME_TIME;
    private int callsInCurrentNaturalTick;
    private int callsInPreviousCompleteNaturalTick;
    private int statisticsActualTicks;
    private int statisticsNaturalTicks;
    private int actualTicks;
    private int naturalTicks;
    private int settledExtraParallelHundredths;
    private int previousActualTicks;
    private int previousExtraParallelHundredths;
    private boolean externalParallelEligible;

    Observation observe(long gameTime, int cycleTicks) {
        boolean newNaturalTick = gameTime != lastGameTime;
        externalParallelEligible = cycleTicks > 0 && cycleTicks <= MINIMUM_NATURAL_TICKS;
        int effectiveCycleTicks = Math.max(MINIMUM_NATURAL_TICKS, cycleTicks);
        int completedExtraParallelHundredths = 0;
        int completedActualTicks = 0;
        if (newNaturalTick) {
            if (lastGameTime != UNSET_GAME_TIME) {
                callsInPreviousCompleteNaturalTick = callsInCurrentNaturalTick;
            }
            lastGameTime = gameTime;
            callsInCurrentNaturalTick = 0;
            naturalTicks = saturatedIncrement(naturalTicks);
            if (statisticsNaturalTicks >= MINIMUM_NATURAL_TICKS) {
                previousActualTicks = statisticsActualTicks;
                previousExtraParallelHundredths =
                        extraParallelForRatio(statisticsActualTicks, statisticsNaturalTicks);
                statisticsActualTicks = 0;
                statisticsNaturalTicks = 0;
            }
            statisticsNaturalTicks = saturatedIncrement(statisticsNaturalTicks);
        }
        callsInCurrentNaturalTick = saturatedIncrement(callsInCurrentNaturalTick);
        statisticsActualTicks = saturatedIncrement(statisticsActualTicks);
        actualTicks = saturatedIncrement(actualTicks);

        boolean reachedCycleTicks = actualTicks >= effectiveCycleTicks;
        boolean complete = reachedCycleTicks && naturalTicks >= MINIMUM_NATURAL_TICKS;
        if (complete) {
            completedExtraParallelHundredths =
                    externalParallelEligible
                            && statisticsNaturalTicks == MINIMUM_NATURAL_TICKS
                            ? extraParallelForRatio(statisticsActualTicks, statisticsNaturalTicks)
                            : 0;
            settledExtraParallelHundredths = completedExtraParallelHundredths;
            completedActualTicks = actualTicks;
            actualTicks = 0;
            naturalTicks = 0;
            callsInCurrentNaturalTick = 0;
        }
        return new Observation(
                Math.min(actualTicks, effectiveCycleTicks),
                complete,
                completedExtraParallelHundredths,
                completedActualTicks,
                actualTicks,
                naturalTicks);
    }

    int currentActualTicks() {
        return statisticsActualTicks;
    }

    int currentNaturalTicks() {
        return statisticsNaturalTicks;
    }

    int currentCycleNaturalTicks() {
        return naturalTicks;
    }

    int settledExtraParallelHundredths() {
        return settledExtraParallelHundredths;
    }

    int currentExtraParallelHundredths() {
        return externalParallelEligible
                && statisticsActualTicks > 0
                && statisticsNaturalTicks > 0
                && statisticsNaturalTicks <= MINIMUM_NATURAL_TICKS
                // Keep the live value responsive even when the rolling 400-tick
                // statistics window has just been reset. A burst of calls in the
                // current natural tick is itself a valid acceleration sample.
                ? Math.max(
                        estimatedExtraParallelHundredths(),
                        extraParallelForRatio(callsInCurrentNaturalTick, 1))
                : 0;
    }

    int previousActualTicks() {
        return previousActualTicks;
    }

    int previousExtraParallelHundredths() {
        return previousExtraParallelHundredths;
    }

    State save() {
        return new State(
                lastGameTime,
                callsInCurrentNaturalTick,
                callsInPreviousCompleteNaturalTick,
                statisticsActualTicks,
                statisticsNaturalTicks,
                actualTicks,
                naturalTicks,
                settledExtraParallelHundredths,
                previousActualTicks,
                previousExtraParallelHundredths,
                externalParallelEligible);
    }

    void load(State state) {
        lastGameTime = state.lastGameTime();
        callsInCurrentNaturalTick = Math.max(0, state.callsInCurrentNaturalTick());
        callsInPreviousCompleteNaturalTick =
                Math.max(0, state.callsInPreviousCompleteNaturalTick());
        statisticsActualTicks = Math.max(0, state.statisticsActualTicks());
        statisticsNaturalTicks =
                Math.max(0, Math.min(MINIMUM_NATURAL_TICKS, state.statisticsNaturalTicks()));
        actualTicks = Math.max(0, state.actualTicks());
        naturalTicks = Math.max(0, Math.min(MINIMUM_NATURAL_TICKS, state.naturalTicks()));
        settledExtraParallelHundredths = Math.max(0, state.settledExtraParallelHundredths());
        previousActualTicks = Math.max(0, state.previousActualTicks());
        previousExtraParallelHundredths = Math.max(0, state.previousExtraParallelHundredths());
        externalParallelEligible = state.externalParallelEligible();
    }

    private int estimatedExtraParallelHundredths() {
        return extraParallelForRatio(statisticsActualTicks, statisticsNaturalTicks);
    }

    private static int extraParallelForRatio(int actualTicks, int naturalTicks) {
        double extraParallel =
                Math.sqrt(Math.max(0.0D, actualTicks / (double) naturalTicks - 1.0D));
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(extraParallel * 100.0D));
    }

    private static int saturatedIncrement(int value) {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }

    record Observation(
            int progressTicks,
            boolean complete,
            int completedExtraParallelHundredths,
            int completedActualTicks,
            int actualTicks,
            int naturalTicks) {}

    record State(
            long lastGameTime,
            int callsInCurrentNaturalTick,
            int callsInPreviousCompleteNaturalTick,
            int statisticsActualTicks,
            int statisticsNaturalTicks,
            int actualTicks,
            int naturalTicks,
            int settledExtraParallelHundredths,
            int previousActualTicks,
            int previousExtraParallelHundredths,
            boolean externalParallelEligible) {}
}
