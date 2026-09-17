package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The compact one-to-many copy workflow. */
final class StructureDataOperatorOperationPage {
    private StructureDataOperatorOperationPage() {}

    static void render(StructureDataOperatorScreen s, GuiGraphics g, int mouseX, int mouseY) {
        button(
                s,
                g,
                StructureDataOperatorScreen.OPERATE_COPY_X,
                StructureDataOperatorScreen.OPERATE_ACTION_Y,
                StructureDataOperatorScreen.OPERATE_COPY_W,
                StructureDataOperatorScreen.OPERATE_ACTION_H,
                "screen.dimension_tech.structure_operator.copy",
                canCopy(s),
                mouseX,
                mouseY);
        button(
                s,
                g,
                StructureDataOperatorScreen.OPERATE_CLEAR_X,
                StructureDataOperatorScreen.OPERATE_ACTION_Y,
                StructureDataOperatorScreen.OPERATE_CLEAR_W,
                StructureDataOperatorScreen.OPERATE_ACTION_H,
                "screen.dimension_tech.structure_operator.clear",
                s.hasOperands(),
                mouseX,
                mouseY);
    }

    static boolean mouseClicked(StructureDataOperatorScreen s, int x, int y) {
        if (StructureDataOperatorScreen.inside(
                        x,
                        y,
                        StructureDataOperatorScreen.OPERATE_COPY_X,
                        StructureDataOperatorScreen.OPERATE_ACTION_Y,
                        StructureDataOperatorScreen.OPERATE_COPY_W,
                        StructureDataOperatorScreen.OPERATE_ACTION_H)
                && canCopy(s)) {
            s.showCopyConfirmation();
            return true;
        }
        if (StructureDataOperatorScreen.inside(
                        x,
                        y,
                        StructureDataOperatorScreen.OPERATE_CLEAR_X,
                        StructureDataOperatorScreen.OPERATE_ACTION_Y,
                        StructureDataOperatorScreen.OPERATE_CLEAR_W,
                        StructureDataOperatorScreen.OPERATE_ACTION_H)
                && s.hasOperands()) {
            s.clearOperands();
            return true;
        }
        return false;
    }

    private static boolean canCopy(StructureDataOperatorScreen s) {
        return s.hasWriteMarker()
                && s.hasOperands()
                && com.suntide_20210418.dimensiontech.item.StructMarkerItem.getMarkerInfo(
                                s.menu().getSlot(StructureDataOperatorBlockEntity.TARGET).getItem())
                        .isPresent();
    }

    private static void button(
            StructureDataOperatorScreen s,
            GuiGraphics g,
            int x,
            int y,
            int w,
            int h,
            String key,
            boolean enabled,
            int mouseX,
            int mouseY) {
        MythicMinerTheme.button(
                g,
                s.getMinecraft().font,
                x,
                y,
                w,
                h,
                Component.translatable(key),
                StructureDataOperatorScreen.inside(mouseX, mouseY, x, y, w, h),
                enabled,
                MythicMinerTheme.FLUID_ACCENT);
    }
}
