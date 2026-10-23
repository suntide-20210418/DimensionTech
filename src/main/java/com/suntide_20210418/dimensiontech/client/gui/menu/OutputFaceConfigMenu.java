package com.suntide_20210418.dimensiontech.client.gui.menu;

import com.suntide_20210418.dimensiontech.client.gui.screen.OutputFaceConfigScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The minimal surface {@link OutputFaceConfigScreen} needs to render and edit a machine's per-side
 * fluid/output faces. Both the miner and the reactor menus implement it, so the same screen is
 * reused without a heavier abstraction.
 */
public interface OutputFaceConfigMenu {
    int containerId();

    /** Whether the given (logical) side is configured to accept output. */
    boolean isOutputFaceEnabled(Direction d);

    /** Whether the given (logical) side is configured to accept input fluid. */
    boolean isInputFaceEnabled(Direction d);

    /** Whether the "modern/AE-style output" mode is active. */
    boolean isModernModeEnabled();

    /** Whether auto-extract (pull fluid from a neighbour) is active. */
    boolean isAutoExtractEnabled();

    /** Cycle the output/fluid-face on the given (logical) side. */
    void cycleOutputFace(Direction d);

    /** Toggle the modern/AE-style output mode. */
    void cycleModernMode();

    /** Toggle auto-extract fluid. */
    void cycleAutoExtract();

    BlockPos getBlockPos();

    /** Map a logical/machine-local direction to a world direction. */
    Direction toWorldDirection(Direction d);
}
