package com.suntide_20210418.dimensiontech.client.gui.screen;

/** A logical rectangle used by both rendering and input dispatch. */
public record GuiRect(int x, int y, int width, int height) {
    public GuiRect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("GUI rectangle dimensions cannot be negative");
        }
    }

    public boolean contains(int localX, int localY) {
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean intersects(GuiRect other) {
        return other != null
                && x < other.right()
                && right() > other.x()
                && y < other.bottom()
                && bottom() > other.y();
    }
}
