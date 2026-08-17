package com.suntide_20210418.dimensiontech.utils.loot.expectation;

/**
 * Probability contract for structure-value analysis.
 *
 * <p>Each Minecraft 1.20.1 {@code RandomSource} method consumes a fresh ideal finite draw of the
 * width used by that method. Bounded integers retain the source rejection algorithm, while
 * conditional execution retains the complete ordered call path. This is the probability model in
 * which a weighted selection of 12 out of 137 has probability exactly {@code 12/137}.
 */
public final class IdealRandomProbabilitySpace1201 {
    public static final String ID = "minecraft_1_20_1_randomsource_ideal_finite_v1";

    private IdealRandomProbabilitySpace1201() {}
}
