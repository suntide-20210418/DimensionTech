package com.suntide_20210418.dimensiontech.client.gui.screen;

/**
 * Maps a marker's progress onto a strip of pixels.
 *
 * <p>Kept static and dependency-free so the rounding rules — which is where progress bars usually go
 * wrong — can be asserted by a plain unit test instead of by looking at the screen.
 */
final class StructureMinerProgressStrip {
    private StructureMinerProgressStrip() {}

    /**
     * Pixels of {@code width} to fill for {@code progress} out of {@code processingTime}.
     *
     * <p>{@code progress} is a logical counter that keeps climbing across cycles, so it is clamped to
     * a single cycle before scaling. Without that clamp a slot that has been running for several
     * cycles would ask for a width past the end of the strip.
     *
     * @return a value in {@code [0, width]}; 0 whenever the strip cannot be measured
     */
    static int pixels(long progress, long processingTime, int width) {
        if (width <= 0 || processingTime <= 0L || progress <= 0L) return 0;
        long clamped = Math.min(progress, processingTime);
        return (int) Math.min(width, clamped * width / processingTime);
    }
}
