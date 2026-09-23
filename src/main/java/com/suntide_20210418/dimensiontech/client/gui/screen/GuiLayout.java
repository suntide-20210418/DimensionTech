package com.suntide_20210418.dimensiontech.client.gui.screen;

import java.util.List;

/**
 * Immutable logical geometry shared by a machine menu and its screen.
 *
 * <p>All coordinates in this type are logical GUI pixels. A screen may apply a UI scale at its
 * outermost pose, but pages must never scale or re-compute these values independently. Keeping the
 * hit rectangles beside the painted rectangles is what prevents the common one-pixel drift between
 * a slot, its chrome and its click target.
 */
public record GuiLayout(
        int width,
        int height,
        GuiRect titleBand,
        GuiRect tabs,
        GuiRect body,
        GuiRect playerInventory,
        List<SlotRect> machineSlots) {
    public GuiLayout {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("GUI dimensions must be positive");
        }
        machineSlots = List.copyOf(machineSlots == null ? List.of() : machineSlots);
    }

    /** Returns a layout with no machine slots, useful for a page before slot geometry is known. */
    public static GuiLayout empty(int width, int height) {
        return new GuiLayout(
                width,
                height,
                new GuiRect(0, 0, width, GuiChrome.BAND_HEIGHT),
                new GuiRect(8, 18, Math.max(0, width - 16), 13),
                new GuiRect(8, 36, Math.max(0, width - 16), Math.max(0, height - 36)),
                new GuiRect(8, Math.max(0, height - 76), Math.max(0, width - 16), 68),
                List.of());
    }
}
