package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact vanilla-style operational screen; fluid levels and current state stay synchronized through menu data. */
public final class MythicCrucibleScreen extends AbstractContainerScreen<MythicCrucibleMenu> {
    public MythicCrucibleScreen(MythicCrucibleMenu menu, Inventory player, Component title) { super(menu, player, title); imageWidth = 176; imageHeight = 166; }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(.12F, .10F, .16F, 1F);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE241C2B);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + imageWidth - 7, topPos + 76, 0xFF3C3048);
        int input = menu.inputAmount() * 48 / 16_000;
        int output = menu.outputAmount() * 48 / 16_000;
        graphics.fill(leftPos + 24, topPos + 70 - input, leftPos + 38, topPos + 70, 0xFF3988B8);
        graphics.fill(leftPos + 138, topPos + 70 - output, leftPos + 152, topPos + 70, 0xFFB35A91);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) { renderBackground(graphics); super.render(graphics, mouseX, mouseY, partial); renderTooltip(graphics, mouseX, mouseY); }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, Component.literal("State: " + menu.currentState()), 8, 22, 0xD8CBE8, false);
        graphics.drawString(font, Component.literal(menu.stateTicks() + "/100"), 8, 34, 0xD8CBE8, false);
    }
}
