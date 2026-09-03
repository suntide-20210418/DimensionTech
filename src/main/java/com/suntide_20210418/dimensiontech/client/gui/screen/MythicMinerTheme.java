package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared AE2-inspired pixel palette and primitives for every miner screen. */
final class MythicMinerTheme {
    static final int FRAME = 0xFF4D566A;
    static final int PANEL = 0xFF68748A;
    static final int INSET = 0xFF3D475B;
    static final int EDGE = 0xFF252B38;
    static final int TEXT = 0xFFD4D9E2;
    static final int MUTED = 0xFFA3ACBC;
    static final int FLUIX = 0xFF70C9E4;
    static final int AMBER = 0xFFC8943C;
    static final int ERROR = 0xFFC6554D;
    static final int SUCCESS = 0xFF6EA27F;

    static final int SLOT_HIGHLIGHT = 0xFF8B96A9;
    static final int FRAME_HIGHLIGHT = 0xFF758196;
    static final int HOVER = 0xFF78859A;
    static final int DISABLED_OVERLAY = 0x770B0F16;
    static final int BACKDROP = 0xFF11161F;
    static final int FLUID = 0xFF5DAFC8;
    // AE2 energy meters use a repeating two-row pixel pattern, not a gradient.
    static final int ENERGY_BORDER = 0xFFC7C9D3;
    static final int ENERGY_BASE_LIGHT = 0xFF696D88;
    static final int ENERGY_BASE_DARK = 0xFF413F54;
    static final int ENERGY_BASE_MID = 0xFF4D4D67;
    static final int ENERGY_FILL_LIGHT = 0xFF915DCD;
    static final int ENERGY_FILL_BRIGHT = 0xFFB06FDD;
    static final int ENERGY_FILL_HOT = 0xFFFF80D7;
    static final int ENERGY_FILL_DARK = 0xFF373B72;
    static final int ENERGY_FILL_MID = 0xFF6054A6;

    private MythicMinerTheme() {}

    static void panel(GuiGraphics g, int x, int y, int width, int height, int accent) {
        g.fill(x, y, x + width, y + height, FRAME);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, EDGE);
        g.fill(x + 2, y + 2, x + width - 2, y + 3, accent);
        g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, EDGE);
        g.fill(x + 1, y + 3, x + 2, y + height - 2, FRAME_HIGHLIGHT);
        g.fill(x + width - 2, y + 3, x + width - 1, y + height - 2, EDGE);
    }

    static void slot(GuiGraphics g, int x, int y, boolean selected, boolean disabled) {
        int edge = selected ? FLUIX : EDGE;
        g.fill(x - 1, y - 1, x + 17, y + 17, edge);
        g.fill(x, y, x + 16, y + 16, disabled ? INSET : PANEL);
        g.fill(x, y, x + 16, y + 1, SLOT_HIGHLIGHT);
        g.fill(x, y + 1, x + 1, y + 15, FRAME_HIGHLIGHT);
        g.fill(x, y + 15, x + 16, y + 16, EDGE);
        g.fill(x + 15, y + 1, x + 16, y + 15, EDGE);
        if (disabled) g.fill(x, y, x + 16, y + 16, DISABLED_OVERLAY);
    }

    static void tab(
            GuiGraphics g, Font font, int x, int y, int width, Component label, boolean selected) {
        g.fill(x, y, x + width, y + 13, selected ? PANEL : INSET);
        g.fill(x, y, x + width, y + 2, selected ? FLUIX : EDGE);
        g.drawCenteredString(font, label, x + width / 2, y + 3, selected ? TEXT : MUTED);
    }

    static void button(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component label,
            boolean hovered,
            boolean enabled,
            int accent) {
        int face = hovered && enabled ? HOVER : PANEL;
        g.fill(x, y, x + width, y + height, EDGE);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, face);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, enabled ? accent : FRAME_HIGHLIGHT);
        g.fill(x + 1, y + 2, x + 2, y + height - 2, FRAME_HIGHLIGHT);
        g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, EDGE);
        g.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, EDGE);
        g.drawCenteredString(
                font, label, x + width / 2, y + (height - 8) / 2, enabled ? TEXT : MUTED);
    }

    static void progress(GuiGraphics g, int x, int y, int width, long value, long max, int accent) {
        g.fill(x, y, x + width, y + 3, INSET);
        int filled = max <= 0 ? 0 : (int) Math.max(0, Math.min(width, width * value / max));
        if (filled > 0) g.fill(x, y, x + filled, y + 3, accent);
        if (filled < width && filled >= 0)
            g.fill(x + filled, y, Math.min(x + width, x + filled + 1), y + 3, AMBER);
    }
}
