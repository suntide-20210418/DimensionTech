package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Complete game-registry structure source page. */
final class StructureInterpreterPage {
    private StructureInterpreterPage() {}

    static void render(StructureDataOperatorScreen s, GuiGraphics g, int mx, int my) {
        StructureDataIntegratorPage.renderSource(
                s, g, mx, my, StructureMinerTheme.AMBER);
    }
}
