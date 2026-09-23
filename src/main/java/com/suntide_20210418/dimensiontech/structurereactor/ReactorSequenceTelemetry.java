package com.suntide_20210418.dimensiontech.structurereactor;

import java.util.List;
import java.util.Objects;

/**
 * Packs a reactor state sequence into the int words carried by the container data channel. The
 * client can therefore redraw the real ritual order without owning the recipe registry, which the
 * server may extend through KubeJS.
 */
public final class ReactorSequenceTelemetry {
    public static final int BITS_PER_STATE = 2;
    public static final int STATES_PER_WORD = Integer.SIZE / BITS_PER_STATE;
    public static final int WORD_COUNT = 2;
    public static final int MAX_STATES = STATES_PER_WORD * WORD_COUNT;
    private static final int STATE_MASK = (1 << BITS_PER_STATE) - 1;

    private ReactorSequenceTelemetry() {}

    /** Packs up to {@link #MAX_STATES} states; anything beyond the capacity is dropped. */
    public static int[] pack(List<StateId> states) {
        Objects.requireNonNull(states, "states");
        int[] words = new int[WORD_COUNT];
        int limit = Math.min(states.size(), MAX_STATES);
        for (int index = 0; index < limit; index++) {
            StateId state = states.get(index);
            if (state == null) continue;
            words[index / STATES_PER_WORD] |= state.ordinal() << shift(index);
        }
        return words;
    }

    /**
     * Reads a packed state, or {@code null} when the index lies outside the sequence. The explicit
     * length disambiguates an unfilled packed slot from {@link StateId#BRANCH}, whose ordinal is
     * zero.
     */
    public static StateId stateAt(int firstWord, int secondWord, int length, int index) {
        if (index < 0 || index >= length || index >= MAX_STATES) return null;
        int word = index < STATES_PER_WORD ? firstWord : secondWord;
        int ordinal = (word >>> shift(index)) & STATE_MASK;
        StateId[] values = StateId.values();
        return ordinal < values.length ? values[ordinal] : null;
    }

    private static int shift(int index) {
        return (index % STATES_PER_WORD) * BITS_PER_STATE;
    }
}
