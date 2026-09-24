package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * A vanilla {@link EditBox} in this mod's colours, tall enough for a Chinese glyph.
 *
 * <p>A bordered {@code EditBox} paints its own frame in vanilla's colours — a near-black fill under
 * a grey outline — which does not sit on these panels. So the widget is left unbordered and the
 * frame is drawn here: with {@code bordered} false, {@code EditBox} restricts itself to the text,
 * the caret, the selection highlight and the hint.
 *
 * <p>The text area is exactly one font line, {@link Font#lineHeight}, because that is how tall a
 * Chinese glyph is. Vanilla centres a bordered box against an assumed {@code 8} — the ASCII cap
 * height — so a nine-pixel glyph in the usual twelve-pixel box rides the frame instead of sitting
 * inside it. Here the padding surrounds a whole line instead.
 *
 * <p>{@code renderWidget} is overridden wholesale. Vanilla draws the value through the
 * five-argument {@code drawString}, which hard-codes {@code dropShadow = true} — and {@code
 * setTextShadow} does not exist on Forge 1.20.1 (it only landed in NeoForge 1.21). That shadow is a
 * second, offset copy of every glyph, which reads as doubled text on the flat panel faces. Drawing
 * the string here with {@code false} removes it while keeping the caret and the hint faithful to
 * the rest of the UI.
 */
final class GuiSearchField extends EditBox {
    /** Room between the drawn frame and the text area. */
    private static final int PAD_X = 3;

    private static final int PAD_Y = 2;

    /** The vanilla font's line height: one row of text, Chinese included. */
    static final int TEXT_HEIGHT = 9;

    private final Font face;
    private final Component hint;

    private GuiSearchField(Font font, int textX, int textY, int textWidth, Component hint) {
        super(font, textX, textY, textWidth, font.lineHeight, hint);
        this.face = font;
        this.hint = hint;
        setBordered(false);
        setTextColor(StructureMinerTheme.INK);
    }

    /** Positions the field by its frame, which is what the layout arithmetic reasons about. */
    static GuiSearchField framed(
            Font font, int frameX, int frameY, int frameWidth, Component hint) {
        return new GuiSearchField(
                font, frameX + PAD_X, frameY + PAD_Y, frameWidth - PAD_X * 2, hint);
    }

    /** Moves the field by its frame, matching {@link #framed}. */
    void moveFrame(int frameX, int frameY) {
        setX(frameX + PAD_X);
        setY(frameY + PAD_Y);
    }

    /** Height of the drawn frame. {@link #TEXT_HEIGHT} must track {@code Font#lineHeight}. */
    static int frameHeight() {
        return TEXT_HEIGHT + PAD_Y * 2;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // The recessed frame, in the canvas' own green family so it belongs to the recess.
        GuiChrome.recessWell(
                g, getX() - PAD_X, getY() - PAD_Y, width + PAD_X * 2, height + PAD_Y * 2);

        int textX = getX();
        int textY = getY();
        int ink = StructureMinerTheme.INK;
        String value = getValue();

        if (!value.isEmpty()) {
            // Clip to the box so a long query cannot spill onto the tree.
            String shown = face.plainSubstrByWidth(value, width);
            g.drawString(face, shown, textX, textY, ink, false);
        } else if (hint != null) {
            // A dimmed tone of the canvas family: readable, but clearly not typed text.
            g.drawString(face, hint, textX, textY, GuiPalette.RECESS_EDGE, false);
        }

        // Caret: a 1px bar that blinks while the field is focused.
        if (isFocused() && (Util.getMillis() / 300L) % 2L == 0L) {
            int cursor = Math.min(getCursorPosition(), value.length());
            int caretX = Math.min(textX + face.width(value.substring(0, cursor)), textX + width);
            g.fill(caretX, textY - 1, caretX + 1, textY + 1 + face.lineHeight, ink);
        }
    }

    /**
     * The frame belongs to the control, so its padding is clickable too.
     *
     * <p>{@code AbstractWidget.mouseClicked} asks {@code clicked}, not {@code isMouseOver}, and the
     * inherited check measures the text area alone — so both have to widen, or three pixels of
     * visible border would look like part of the control and not respond to it.
     */
    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return overFrame(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && overFrame(mouseX, mouseY);
    }

    private boolean overFrame(double mouseX, double mouseY) {
        return mouseX >= (double) (getX() - PAD_X)
                && mouseX < (double) (getX() + width + PAD_X)
                && mouseY >= (double) (getY() - PAD_Y)
                && mouseY < (double) (getY() + height + PAD_Y);
    }
}
