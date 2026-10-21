package com.suntide_20210418.dimensiontech.client.model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ConnectionMaskTest {
    @Test
    void allNeighbourPatternsReduceToExactly47ReachableStates() {
        assertEquals(47L, IntStream.range(0, 256).map(ConnectionMask::normalize).distinct().count());
        assertEquals(47, ConnectionMask.validMasks().length);
        for (int raw = 0; raw < 256; raw++) {
            int normalized = ConnectionMask.normalize(raw);
            assertEquals(raw & 15, normalized & 15, "side neighbours must survive");
            assertEquals(normalized, ConnectionMask.normalize(normalized), "normalization is idempotent");
        }
    }

    @Test
    void aDiagonalCannotEraseAConcaveCornerWithoutBothEdgeNeighbours() {
        for (int corner = 0; corner < 4; corner++) {
            int first = 1 << corner, second = 1 << ((corner + 1) & 3), diagonal = 16 << corner;
            assertEquals(0, ConnectionMask.normalize(diagonal));
            assertEquals(first, ConnectionMask.normalize(first | diagonal));
            assertEquals(second, ConnectionMask.normalize(second | diagonal));
            assertEquals(first | second | diagonal, ConnectionMask.normalize(first | second | diagonal));
            assertEquals(first | second, ConnectionMask.normalize(first | second));
        }
    }

    @Test
    void rotationPreservesTheSameConnectivityRuleOnAllFourCorners() {
        for (int raw = 0; raw < 256; raw++) {
            assertEquals(rotate(ConnectionMask.normalize(raw)), ConnectionMask.normalize(rotate(raw)));
        }
    }

    private static int rotate(int mask) {
        int edges = mask & 15, corners = (mask >>> 4) & 15;
        return ((edges << 1) | (edges >>> 3)) & 15
                | ((((corners << 1) | (corners >>> 3)) & 15) << 4);
    }
}
