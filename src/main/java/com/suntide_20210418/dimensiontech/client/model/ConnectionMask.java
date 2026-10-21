package com.suntide_20210418.dimensiontech.client.model;

import java.util.stream.IntStream;

/**
 * Clockwise U,R,D,L side bits, then UR,RD,DL,LU corner bits. Exactly 47 reachable states: a corner
 * only connects when both of its side neighbours do.
 */
public final class ConnectionMask {
    private ConnectionMask() {}

    public static int normalize(int mask) {
        int sides = mask & 15;
        int result = sides;
        for (int i = 0; i < 4; i++) {
            int pair = (1 << i) | (1 << ((i + 1) & 3));
            if ((sides & pair) == pair && (mask & (16 << i)) != 0) result |= 16 << i;
        }
        return result;
    }

    public static int[] validMasks() {
        return IntStream.range(0, 256).filter(i -> normalize(i) == i).toArray();
    }
}
