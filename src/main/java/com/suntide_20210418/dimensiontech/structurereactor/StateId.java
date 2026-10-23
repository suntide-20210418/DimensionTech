package com.suntide_20210418.dimensiontech.structurereactor;

/** The only operations understood by the V1 reactor state DSL. */
public enum StateId {
    BRANCH(1),
    RECURSE(2),
    CONVERGE(3),
    STABILIZE(4);

    /**
     * The comparator level this step announces while the reward window is closed.
     *
     * <p>It is spelled out per constant rather than derived from {@link #ordinal()} so that
     * inserting a step later cannot silently renumber a redstone interface that players have
     * already built around. The reward-window half of the range lives in {@link
     * ReactorAnalogSignal}.
     */
    private final int signalLevel;

    StateId(int signalLevel) {
        this.signalLevel = signalLevel;
    }

    /** The level this step announces outside the reward window; always within {@code 1..4}. */
    public int signalLevel() {
        return signalLevel;
    }
}
