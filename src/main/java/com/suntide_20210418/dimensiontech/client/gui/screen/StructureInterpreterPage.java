package com.suntide_20210418.dimensiontech.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Complete game-registry structure source page. */
final class StructureInterpreterPage {
    private StructureInterpreterPage() {}

    static void render(StructureDataOperatorScreen s, GuiGraphics g, int mx, int my) {
        StructureDataIntegratorPage.renderSource(s, g, mx, my, MythicMinerTheme.AMBER,
                "screen.dimension_tech.structure_operator.page.interpreter");
    }
    static boolean mouseClicked(StructureDataOperatorScreen s, int x, int y) { return StructureDataIntegratorPage.mouseClicked(s, x, y); }
}
