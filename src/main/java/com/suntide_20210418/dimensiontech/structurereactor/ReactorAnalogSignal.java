package com.suntide_20210418.dimensiontech.structurereactor;

/**
 * The comparator level a reactor announces for its current position in the ritual.
 *
 * <p>The sixteen levels are partitioned by <em>what the reactor is waiting for</em>, because that
 * is the only thing a redstone circuit can act on. A level never means two things, so a single
 * comparator can be compared, subtracted and latched without knowing which recipe is loaded:
 *
 * <ul>
 *   <li>{@link #IDLE} - nothing to report; the cycle cannot start yet but only because material is
 *       still arriving, which no circuit needs to react to.
 *   <li>{@code 1}-{@code 4} - running an operation step outside the reward window, one level per
 *       {@link StateId} (see {@link StateId#signalLevel()}).
 *   <li>{@code 5}-{@code 8} - the same four steps while the reward window is open, i.e. {@code
 *       level + }{@link #REWARD_WINDOW_OFFSET}. A circuit decodes the step with {@code (signal - 1)
 *       % 4} and the window with {@code signal >= 5}.
 *   <li>{@code 9}-{@code 12} - deliberately unassigned, so a future refinement stays inside the
 *       operation-phase half of the range.
 *   <li>{@link #IDLE_BLOCKED} - idle but blocked: the tank and the fragment slot disagree with
 *       every recipe, or the output tank has no room. Unlike {@link #IDLE} this one needs a player.
 *   <li>{@link #COMMIT_BLOCKED} - refining finished and the commit cannot settle. The cycle spends
 *       zero ticks in that status when the resources are in place, so a comparator only ever sees
 *       this level while the reactor is genuinely stuck.
 *   <li>{@link #REFINING} - refining, waiting for time.
 * </ul>
 *
 * <p>This class is deliberately free of world, inventory and tick state: the caller supplies the
 * cycle snapshot, which keeps the mapping testable without a running game.
 */
public final class ReactorAnalogSignal {
    /** Idle with material still on its way; the resting value of an unused reactor. */
    public static final int IDLE = 0;

    /** Idle with stored material that no recipe accepts, or a full output tank. */
    public static final int IDLE_BLOCKED = 13;

    /** Refining finished but the commit is refused; sticks until the missing resource arrives. */
    public static final int COMMIT_BLOCKED = 14;

    /** Refining, waiting for time. */
    public static final int REFINING = 15;

    /** Added to {@link StateId#signalLevel()} while the reward window is open. */
    public static final int REWARD_WINDOW_OFFSET = 4;

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 15;

    private ReactorAnalogSignal() {}

    /**
     * Maps one cycle snapshot to a comparator level.
     *
     * @param status the cycle status
     * @param currentState the step being resolved, or {@code null} while the cycle is idle
     * @param stateTicks ticks waited on the current step
     * @param startBlocked whether an idle reactor is refused a cycle by its own contents; ignored
     *     for every other status
     * @return a level within {@link #MIN_VALUE}..{@link #MAX_VALUE}
     */
    public static int of(
            StructureReactorCycle.Status status,
            StateId currentState,
            int stateTicks,
            boolean startBlocked) {
        return switch (status) {
            case IDLE -> startBlocked ? IDLE_BLOCKED : IDLE;
            case REFINING -> REFINING;
            case READY_TO_COMMIT -> COMMIT_BLOCKED;
            case RUNNING -> running(currentState, stateTicks);
        };
    }

    /** The reward window is a closed interval, matching the settlement check in the cycle. */
    public static boolean inRewardWindow(int stateTicks) {
        return stateTicks >= StructureReactorCycle.REWARD_START_TICK
                && stateTicks <= StructureReactorCycle.REWARD_END_TICK;
    }

    private static int running(StateId currentState, int stateTicks) {
        // A running cycle always names a step; only an idle one reports null, which is not ours.
        if (currentState == null) return IDLE;
        int level = currentState.signalLevel();
        return inRewardWindow(stateTicks) ? level + REWARD_WINDOW_OFFSET : level;
    }
}
