package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Centred text drawn without a drop shadow.
 *
 * <p>{@code GuiGraphics.drawCenteredString} has no overload that omits the shadow: all three of its
 * variants forward to a five-argument {@code drawString}, which hard-codes {@code dropShadow =
 * true}. Centring a string therefore forced an outline onto it, which reads as unwanted extra
 * weight on these flat panel faces. Every centred string in this mod goes through here instead, so
 * the shadow cannot come back by accident.
 */
final class GuiText {
    private GuiText() {}

    /** Draws {@code text} centred on {@code centerX}, with no drop shadow. */
    static void centered(GuiGraphics g, Font font, Component text, int centerX, int y, int color) {
        g.drawString(
                font, text, centerX - font.width(text.getVisualOrderText()) / 2, y, color, false);
    }
}
