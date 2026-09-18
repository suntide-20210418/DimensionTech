package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.StructureReactorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureReactorLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureReactorMenu;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorTooltipSnapshot;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import com.suntide_20210418.dimensiontech.structurereactor.StateId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Texture-backed Structure Reactor screen. */
public final class StructureReactorScreen extends AbstractContainerScreen<StructureReactorMenu> {
    private static final int TICKS = StructureReactorCycle.STATE_TIMEOUT_TICKS;
    private static final int FLUID_FALLBACK = 0xFF7F879F;
    private static final int TITLE_LABEL_X = 7;
    private static final int TITLE_LABEL_Y = 6;
    private static final int INVENTORY_LABEL_X = StructureReactorLayout.PLAYER_INVENTORY.x();
    private static final int INVENTORY_LABEL_Y = StructureReactorLayout.PLAYER_INVENTORY.y() - 10;
    private static final int LABEL_COLOR = 0xFF413F54;
    private static final int INK = 0xFF000000;
    private static final int SUCCESS = 0xFF2E6B3E;
    private static final int ERROR = 0xFFA6442F;
    private static final int REWARD_TEXT = 0xFF8A5A14;
    private static final int SELECT = 0xFF77909F;
    private static final int FLUIX = 0xFF2A6180;
    static final int SEQUENCE_PREVIOUS_COLOR = 0xFF777783;
    static final int SEQUENCE_CURRENT_COLOR = 0xFFB66CFF;
    static final int SEQUENCE_NEXT_COLOR = 0xFFFFFFFF;
    static final int PROGRESS_REWARD_COLOR = 0xFFFFA33A;
    static final int PROGRESS_TIMEOUT_COLOR = 0xFFFF4A4A;
    private static final int STATUS_AREA_X = StructureReactorLayout.STATUS_REWARD_AREA.x() + 2;
    private static final int STATUS_AREA_Y = StructureReactorLayout.STATUS_REWARD_AREA.y() + 2;
    private static final int STATUS_AREA_WIDTH = StructureReactorLayout.STATUS_REWARD_AREA.width() - 4;
    private static final int DETAIL_LINE_HEIGHT = 11;
    private static final int DETAIL_TEXT_INSET = 3;
    private static final String KEY_TITLE = "screen.dimension_tech.structure_reactor.title";
    private static final String KEY_INVENTORY_LABEL =
            "screen.dimension_tech.structure_reactor.inventory_label";
    private static final String KEY_TOOLTIP_INPUT =
            "screen.dimension_tech.structure_reactor.tooltip.input";
    private static final String KEY_TOOLTIP_OUTPUT =
            "screen.dimension_tech.structure_reactor.tooltip.output";
    private static final String KEY_TOOLTIP_EMPTY =
            "screen.dimension_tech.structure_reactor.tooltip.empty";
    private static final String KEY_TOOLTIP_FLUID =
            "screen.dimension_tech.structure_reactor.tooltip.fluid";
    private static final String KEY_TOOLTIP_AMOUNT =
            "screen.dimension_tech.structure_reactor.tooltip.amount";
    private static final String KEY_TOOLTIP_EXPECTED =
            "screen.dimension_tech.structure_reactor.tooltip.expected";
    private static final String KEY_TOOLTIP_FREE =
            "screen.dimension_tech.structure_reactor.tooltip.free";
    private static final String KEY_TOOLTIP_FRAGMENT =
            "screen.dimension_tech.structure_reactor.tooltip.fragment";
    private static final String KEY_TOOLTIP_OPERATION =
            "screen.dimension_tech.structure_reactor.tooltip.operation";
    private static final String KEY_TOOLTIP_STEP =
            "screen.dimension_tech.structure_reactor.tooltip.step";
    private static final String KEY_TOOLTIP_STATE =
            "screen.dimension_tech.structure_reactor.tooltip.state";
    private static final String KEY_TOOLTIP_CURRENT =
            "screen.dimension_tech.structure_reactor.tooltip.current";
    private static final String KEY_TOOLTIP_EMPTY_ITEM =
            "screen.dimension_tech.structure_reactor.tooltip.empty_item";
    private static final String KEY_TOOLTIP_NEEDS =
            "screen.dimension_tech.structure_reactor.tooltip.needs";
    private static final String KEY_TOOLTIP_MORE =
            "screen.dimension_tech.structure_reactor.tooltip.more";
    private static final String KEY_TOOLTIP_TIMEOUT =
            "screen.dimension_tech.structure_reactor.tooltip.timeout";
    private static final String KEY_FLUID_CLEAR_HINT =
            "screen.dimension_tech.structure_reactor.fluid_clear_hint";
    private static final String KEY_STATUS_PREVIOUS =
            "screen.dimension_tech.structure_reactor.status_line.previous";
    private static final String KEY_STATUS_NO_REQUIREMENT =
            "screen.dimension_tech.structure_reactor.status_line.no_requirement";
    private static final String KEY_REWARD_WINDOW =
            "screen.dimension_tech.structure_reactor.status_line.reward_window";
    private static final String KEY_REWARD_IDLE =
            "screen.dimension_tech.structure_reactor.status_line.reward_idle";
    private static final String KEY_DETAIL_SEQUENCE =
            "screen.dimension_tech.structure_reactor.detail.sequence";
    private static final String KEY_DETAIL_NEEDS =
            "screen.dimension_tech.structure_reactor.detail.needs";
    private static final String KEY_DETAIL_CHANGES =
            "screen.dimension_tech.structure_reactor.detail.changes";
    private static final String KEY_DETAIL_PREVIOUS =
            "screen.dimension_tech.structure_reactor.detail.previous";
    private static final String KEY_DETAIL_FLUID_REQUIRED =
            "screen.dimension_tech.structure_reactor.detail.fluid_required";

    private int detailScroll = 0;
    private int detailContentHeight = 0;
    private boolean draggingScrollbar = false;
    private int scrollGrabY = 0;

    public StructureReactorScreen(StructureReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = StructureReactorLayout.WIDTH;
        imageHeight = StructureReactorLayout.HEIGHT;
        inventoryLabelY = Integer.MIN_VALUE;
        titleLabelY = Integer.MIN_VALUE;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, StructureMinerTheme.BACKDROP);
        g.blit(
                StructureReactorLayout.TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                0,
                StructureReactorLayout.WIDTH,
                StructureReactorLayout.HEIGHT,
                StructureReactorLayout.TEXTURE_WIDTH,
                StructureReactorLayout.TEXTURE_HEIGHT);
        drawFluid(
                g,
                StructureReactorLayout.INPUT_TANK,
                menu.inputFluid(),
                menu.inputAmount(),
                menu.inputCapacity());
        drawFluid(
                g,
                StructureReactorLayout.OUTPUT_TANK,
                menu.outputFluid(),
                menu.outputAmount(),
                menu.outputCapacity());
        drawProgressOverlay(g);
    }

    private void drawFluid(GuiGraphics g, GuiRect tank, Fluid fluid, int amount, int capacity) {
        int innerX = leftPos + StructureReactorLayout.innerX(tank);
        int innerY = topPos + StructureReactorLayout.innerY(tank);
        int innerWidth = StructureReactorLayout.innerWidth(tank);
        int innerHeight = StructureReactorLayout.innerHeight(tank);
        int safeCapacity = Math.max(1, capacity);
        int safeAmount = Math.max(0, Math.min(safeCapacity, amount));
        int filledHeight = progressPixels(safeAmount, safeCapacity, innerHeight);
        boolean rendered = false;
        if (filledHeight > 0 && fluid != null && fluid != Fluids.EMPTY) {
            ResourceLocation still = IClientFluidTypeExtensions.of(fluid).getStillTexture();
            if (still != null) {
                TextureAtlasSprite sprite =
                        Minecraft.getInstance()
                                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                                .apply(still);
                int tint = IClientFluidTypeExtensions.of(fluid).getTintColor();
                g.setColor(
                        ((tint >> 16) & 0xFF) / 255.0F,
                        ((tint >> 8) & 0xFF) / 255.0F,
                        (tint & 0xFF) / 255.0F,
                        1.0F);
                int fluidTop = innerY + innerHeight - filledHeight;
                g.enableScissor(innerX, fluidTop, innerX + innerWidth, innerY + innerHeight);
                for (int y = fluidTop; y < innerY + innerHeight; y += 16) {
                    for (int x = innerX; x < innerX + innerWidth; x += 16) {
                        g.blit(x, y, 0, 16, 16, sprite);
                    }
                }
                g.disableScissor();
                g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                rendered = true;
            }
        }
        if (filledHeight > 0 && !rendered) {
            g.fill(
                    innerX,
                    innerY + innerHeight - filledHeight,
                    innerX + innerWidth,
                    innerY + innerHeight,
                    FLUID_FALLBACK);
        }
        int scaleX = leftPos + tank.x() + tank.width() - StructureReactorLayout.FLUID_SCALE_WIDTH - 1;
        int scaleY =
                topPos + tank.y() + (tank.height() - StructureReactorLayout.FLUID_SCALE_HEIGHT) / 2;
        g.blit(
                StructureReactorLayout.TEXTURE,
                scaleX,
                scaleY,
                0,
                StructureReactorLayout.FLUID_SCALE_U,
                StructureReactorLayout.FLUID_SCALE_V,
                StructureReactorLayout.FLUID_SCALE_WIDTH,
                StructureReactorLayout.FLUID_SCALE_HEIGHT,
                StructureReactorLayout.TEXTURE_WIDTH,
                StructureReactorLayout.TEXTURE_HEIGHT);
    }

    static int progressPixels(long value, long max, int pixels) {
        if (max <= 0 || pixels <= 0) return 0;
        return (int) Math.max(0, Math.min(pixels, value * pixels / max));
    }

    private void drawProgressOverlay(GuiGraphics g) {
        int status = menu.status();
        long value;
        long maximum;
        if (status == StructureReactorCycle.Status.REFINING.ordinal()) {
            value = menu.refiningTicks();
            maximum = menu.resultTimeTicks();
        } else if (status == StructureReactorCycle.Status.RUNNING.ordinal()) {
            value = menu.stateTicks();
            maximum = TICKS;
        } else if (status == StructureReactorCycle.Status.READY_TO_COMMIT.ordinal()) {
            value = 1;
            maximum = 1;
        } else {
            value = 0;
            maximum = 1;
        }
        int covered = progressPixels(value, maximum, StructureReactorLayout.PROGRESS_BAR.height());
        if (covered <= 0) return;
        GuiRect bar = StructureReactorLayout.PROGRESS_BAR;
        g.blit(
                StructureReactorLayout.TEXTURE,
                leftPos + bar.x(),
                topPos + bar.y(),
                0,
                StructureReactorLayout.PROGRESS_OVERLAY_U,
                StructureReactorLayout.PROGRESS_OVERLAY_V,
                StructureReactorLayout.PROGRESS_OVERLAY_WIDTH,
                covered,
                StructureReactorLayout.TEXTURE_WIDTH,
                StructureReactorLayout.TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(
                font,
                Component.translatable(KEY_TITLE),
                TITLE_LABEL_X,
                TITLE_LABEL_Y,
                LABEL_COLOR,
                false);
        g.drawString(
                font,
                Component.translatable(KEY_INVENTORY_LABEL),
                INVENTORY_LABEL_X,
                INVENTORY_LABEL_Y,
                LABEL_COLOR,
                false);
        renderStatusReward(g);
        renderDetail(g);
    }

    /** Compact readout on the machine face: current status + progress, and the reward window. */
    private void renderStatusReward(GuiGraphics g) {
        StructureReactorCycle.Status status = statusValue(menu.status());
        int current;
        int maximum;
        if (status == StructureReactorCycle.Status.REFINING) {
            current = Math.max(0, menu.refiningTicks());
            maximum = Math.max(1, menu.resultTimeTicks());
        } else if (status == StructureReactorCycle.Status.RUNNING) {
            current = clampTicks(menu.stateTicks());
            maximum = TICKS;
        } else if (status == StructureReactorCycle.Status.READY_TO_COMMIT) {
            current = TICKS;
            maximum = TICKS;
        } else {
            current = 0;
            maximum = 0;
        }
        String line =
                statusText(status).getString()
                        + " "
                        + current
                        + (maximum > 0 ? "/" + maximum : "");
        g.drawString(font, Component.literal(line), STATUS_AREA_X, STATUS_AREA_Y, INK, false);

        boolean inWindow =
                status == StructureReactorCycle.Status.RUNNING
                        && menu.stateTicks() >= StructureReactorCycle.REWARD_START_TICK
                        && menu.stateTicks() <= StructureReactorCycle.REWARD_END_TICK;
        Component reward =
                inWindow
                        ? Component.translatable(
                                KEY_REWARD_WINDOW,
                                StructureReactorCycle.REWARD_START_TICK,
                                StructureReactorCycle.REWARD_END_TICK)
                        : Component.translatable(KEY_REWARD_IDLE);
        g.drawString(
                font,
                reward,
                STATUS_AREA_X,
                STATUS_AREA_Y + 13,
                inWindow ? REWARD_TEXT : INK,
                false);
    }

    /** Scrollable detail panel: sequence, step needs, recipe changes and last settlement. */
    private void renderDetail(GuiGraphics g) {
        List<DetailLine> lines = buildDetailLines();
        detailContentHeight = lines.size() * DETAIL_LINE_HEIGHT;
        clampDetailScroll();
        int vx = leftPos + StructureReactorLayout.VIEWPORT.x();
        int vy = topPos + StructureReactorLayout.VIEWPORT.y();
        int vw = StructureReactorLayout.VIEWPORT.width();
        int vh = StructureReactorLayout.VIEWPORT.height();
        g.enableScissor(vx, vy, vx + vw, vy + vh);
        int baseY = vy + DETAIL_TEXT_INSET;
        int x = vx + DETAIL_TEXT_INSET;
        for (int i = 0; i < lines.size(); i++) {
            int y = baseY + i * DETAIL_LINE_HEIGHT - detailScroll;
            if (y + DETAIL_LINE_HEIGHT < vy || y > vy + vh) continue;
            DetailLine line = lines.get(i);
            if (line.highlight()) {
                g.fill(vx, y - 1, vw, DETAIL_LINE_HEIGHT, SELECT);
            }
            g.drawString(font, line.text(), x, y, line.color(), false);
        }
        g.disableScissor();
        drawDetailScrollbar(g);
    }

    private List<DetailLine> buildDetailLines() {
        List<DetailLine> lines = new ArrayList<>();
        lines.add(DetailLine.header(Component.translatable(KEY_DETAIL_SEQUENCE)));
        StructureReactorCycle.Status status = statusValue(menu.status());
        int currentIndex = status == StructureReactorCycle.Status.RUNNING ? menu.stateIndex() : -1;
        int length = Math.max(0, Math.min(menu.sequenceLength(), 32));
        for (int i = 0; i < length; i++) {
            StateId state = menu.sequenceState(i);
            if (state == null) continue;
            boolean current = i == currentIndex;
            Component text = Component.literal(current ? "▶ " : "· ").append(stageText(state));
            int color = current ? INK : (i < currentIndex ? SEQUENCE_PREVIOUS_COLOR : INK);
            lines.add(new DetailLine(text, color, current));
        }

        lines.add(DetailLine.header(Component.translatable(KEY_DETAIL_NEEDS)));
        ReactorTooltipSnapshot snapshot = menu.tooltipSnapshot();
        if (snapshot.operationCandidates().isEmpty()) {
            lines.add(new DetailLine(Component.translatable(KEY_STATUS_NO_REQUIREMENT), INK, false));
        } else {
            for (ItemStack candidate : snapshot.operationCandidates())
                lines.add(
                        new DetailLine(
                                Component.literal("· " + candidate.getHoverName().getString()),
                                INK,
                                false));
            int total = snapshot.operationCandidateTotal();
            if (total > snapshot.operationCandidates().size())
                lines.add(
                        new DetailLine(
                                Component.literal(
                                        "还有 " + (total - snapshot.operationCandidates().size()) + " 种"),
                                INK,
                                false));
        }
        lines.add(
                new DetailLine(
                        Component.translatable(
                                KEY_DETAIL_FLUID_REQUIRED,
                                Math.max(0, menu.inputAmount()),
                                menu.inputCapacity(),
                                snapshot.inputRequiredAmount()),
                        FLUIX,
                        false));

        lines.add(DetailLine.header(Component.translatable(KEY_DETAIL_CHANGES)));
        int outputChange = menu.outputBonusBp() - menu.outputPenaltyBp();
        int consumptionChange = menu.fluidReductionBp() - menu.fluidPenaltyBp();
        lines.add(
                new DetailLine(
                        Component.literal("产出 " + signedPercent(outputChange)),
                        outputChange >= 0 ? SUCCESS : ERROR,
                        false));
        lines.add(
                new DetailLine(
                        Component.literal("流体 " + signedPercent(-consumptionChange)),
                        consumptionChange >= 0 ? SUCCESS : ERROR,
                        false));
        lines.add(
                new DetailLine(
                        Component.literal("耗时 -" + Math.max(0, menu.timeReduction()) + "t"),
                        INK,
                        false));
        lines.add(
                new DetailLine(
                        Component.literal("碎片 +" + Math.max(0, menu.extraFragments())),
                        INK,
                        false));

        lines.add(DetailLine.header(Component.translatable(KEY_DETAIL_PREVIOUS)));
        lines.add(new DetailLine(previousStatusText(), INK, false));
        return lines;
    }

    private void drawDetailScrollbar(GuiGraphics g) {
        int vx = leftPos + StructureReactorLayout.VIEWPORT.x();
        int vy = topPos + StructureReactorLayout.VIEWPORT.y();
        int vw = StructureReactorLayout.VIEWPORT.width();
        int vh = StructureReactorLayout.VIEWPORT.height();
        int trackX = vx + vw - 2;
        g.fill(trackX, vy, 3, vh, 0x40000000);
        int maxScroll = Math.max(0, detailContentHeight - vh);
        if (maxScroll <= 0) return;
        int thumbH = Math.max(8, vh * vh / Math.max(1, detailContentHeight));
        int thumbY = vy + (vh - thumbH) * detailScroll / maxScroll;
        g.fill(trackX, thumbY, 3, thumbH, 0xFF888888);
    }

    private void clampDetailScroll() {
        int maxScroll =
                Math.max(0, detailContentHeight - StructureReactorLayout.VIEWPORT.height());
        if (detailScroll < 0) detailScroll = 0;
        if (detailScroll > maxScroll) detailScroll = maxScroll;
    }

    private boolean isOverDetail(double mouseX, double mouseY) {
        int lx = (int) mouseX - leftPos;
        int ly = (int) mouseY - topPos;
        GuiRect vp = StructureReactorLayout.VIEWPORT;
        return lx >= vp.x() - 4
                && lx <= vp.x() + vp.width() + 2
                && ly >= vp.y()
                && ly <= vp.y() + vp.height();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isOverDetail(mouseX, mouseY)) {
            detailScroll -= (int) Math.signum(delta) * DETAIL_LINE_HEIGHT;
            clampDetailScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int lx = (int) mouseX - leftPos;
            int ly = (int) mouseY - topPos;
            GuiRect vp = StructureReactorLayout.VIEWPORT;
            if (lx >= vp.x() + vp.width() - 4
                    && lx <= vp.x() + vp.width() + 2
                    && ly >= vp.y()
                    && ly <= vp.y() + vp.height()) {
                draggingScrollbar = true;
                int maxScroll = Math.max(0, detailContentHeight - vp.height());
                if (maxScroll > 0) {
                    int vh = vp.height();
                    int thumbH = Math.max(8, vh * vh / Math.max(1, detailContentHeight));
                    int thumbTop =
                            topPos + vp.y() + (vh - thumbH) * detailScroll / maxScroll;
                    scrollGrabY = (int) mouseY - thumbTop;
                } else {
                    scrollGrabY = 0;
                }
                return true;
            }
            boolean clearing = hasShiftDown();
            if (StructureReactorLayout.INPUT_TANK.contains(lx, ly)) {
                pressButton(
                        clearing
                                ? StructureReactorBlockEntity.BUTTON_CLEAR_INPUT_TANK
                                : StructureReactorBlockEntity.BUTTON_TOGGLE_INPUT_LOCK);
                return true;
            }
            if (clearing && StructureReactorLayout.OUTPUT_TANK.contains(lx, ly)) {
                pressButton(StructureReactorBlockEntity.BUTTON_CLEAR_OUTPUT_TANK);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            GuiRect vp = StructureReactorLayout.VIEWPORT;
            int maxScroll = Math.max(0, detailContentHeight - vp.height());
            if (maxScroll > 0) {
                int vh = vp.height();
                int thumbH = Math.max(8, vh * vh / Math.max(1, detailContentHeight));
                int trackTop = topPos + vp.y();
                int desiredThumbTop = (int) mouseY - scrollGrabY;
                int rel = desiredThumbTop - trackTop;
                detailScroll = rel * maxScroll / Math.max(1, vh - thumbH);
                clampDetailScroll();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int maxScroll =
                Math.max(0, detailContentHeight - StructureReactorLayout.VIEWPORT.height());
        if (keyCode == 266) {
            detailScroll = 0;
            return true;
        }
        if (keyCode == 269) {
            detailScroll = maxScroll;
            return true;
        }
        if (keyCode == 268) {
            detailScroll -= StructureReactorLayout.VIEWPORT.height();
            clampDetailScroll();
            return true;
        }
        if (keyCode == 267) {
            detailScroll += StructureReactorLayout.VIEWPORT.height();
            clampDetailScroll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static String signedPercent(int basisPoints) {
        long magnitude = Math.abs((long) basisPoints);
        long whole = magnitude / 100;
        int fraction = (int) (magnitude % 100);
        String value = Long.toString(whole);
        if (fraction != 0) {
            String fractionText = fraction < 10 ? "0" + fraction : Integer.toString(fraction);
            while (fractionText.endsWith("0"))
                fractionText = fractionText.substring(0, fractionText.length() - 1);
            value += "." + fractionText;
        }
        return (basisPoints > 0 ? "+" : basisPoints < 0 ? "-" : "") + value + "%";
    }

    private Component previousStatusText() {
        StructureReactorCycle.Resolution resolution = menu.lastResolution();
        if (resolution == null || resolution == StructureReactorCycle.Resolution.NONE)
            return Component.translatable(
                    KEY_STATUS_PREVIOUS,
                    Component.translatable(
                            "screen.dimension_tech.structure_reactor.status_line.previous.none"));
        Component step =
                menu.eventState() == null
                        ? Component.translatable(KEY_STATUS_NO_REQUIREMENT)
                        : stageText(menu.eventState());
        String detailKey =
                switch (resolution) {
                    case CORRECT_REWARDED -> "reward";
                    case CORRECT -> "normal";
                    case PHASE_IDLE,
                            EARLY_CONVERGE,
                            RECURSION_OVERFLOW,
                            BRANCH_CONFLICT,
                            STABILIZE_FAILURE ->
                            "penalty";
                    default -> "none";
                };
        Component detail =
                switch (resolution) {
                    case CORRECT_REWARDED -> rewardDetail(menu.eventState());
                    case CORRECT ->
                            Component.translatable(
                                    "screen.dimension_tech.structure_reactor.status_line.previous.normal");
                    case PHASE_IDLE ->
                            Component.translatable(
                                    "screen.dimension_tech.structure_reactor.status_line.previous.penalty.phase_idle",
                                    penaltyTicks(
                                            StructureReactorCycle.WRONG_STATE_TIME_PENALTY_TICKS));
                    case BRANCH_CONFLICT ->
                            Component.translatable(
                                    "screen.dimension_tech.structure_reactor.status_line.previous.penalty.branch",
                                    penaltyTicks(
                                            StructureReactorCycle
                                                    .BRANCH_CONFLICT_TIME_PENALTY_TICKS));
                    case RECURSION_OVERFLOW ->
                            Component.translatable(
                                    "screen.dimension_tech.structure_reactor.status_line.previous.penalty.recurse",
                                    StructureReactorCycle.RECURSION_OVERFLOW_FLUID_PENALTY_BP / 100);
                    case EARLY_CONVERGE ->
                            Component.translatable(
                                    "screen.dimension_tech.structure_reactor.status_line.previous.penalty.converge",
                                    StructureReactorCycle.EARLY_CONVERGE_OUTPUT_PENALTY_BP_PER_DEPTH
                                            / 100);
                    case STABILIZE_FAILURE ->
                            Component.translatable(
                                    "screen.dimension_tech.structure_reactor.status_line.previous.penalty.stabilize",
                                    Math.max(0, menu.extraFragments()));
                    default ->
                            Component.translatable(
                                    "screen.dimension_tech.structure_reactor.status_line.previous.none");
                };
        return Component.translatable(
                KEY_STATUS_PREVIOUS,
                Component.translatable(
                        "screen.dimension_tech.structure_reactor.status_line.previous." + detailKey,
                        step,
                        detail));
    }

    private Component rewardDetail(StateId state) {
        if (state == null)
            return Component.translatable(
                    "screen.dimension_tech.structure_reactor.status_line.previous.reward");
        return switch (state) {
            case BRANCH ->
                    Component.translatable(
                            "screen.dimension_tech.structure_reactor.status_line.previous.reward.branch",
                            Math.max(0, menu.timeReduction()));
            case RECURSE ->
                    Component.translatable(
                            "screen.dimension_tech.structure_reactor.status_line.previous.reward.recurse",
                            Math.max(0, menu.fluidReductionBp()) / 100);
            case CONVERGE ->
                    Component.translatable(
                            "screen.dimension_tech.structure_reactor.status_line.previous.reward.converge",
                            Math.max(0, menu.outputBonusBp()) / 100);
            case STABILIZE ->
                    Component.translatable(
                            "screen.dimension_tech.structure_reactor.status_line.previous.reward.stabilize");
        };
    }

    private int penaltyTicks(int fallback) {
        int total = Math.max(0, menu.timePenalty());
        return total == 0 ? fallback : total;
    }

    private Component statusText(StructureReactorCycle.Status status) {
        return Component.translatable(
                "screen.dimension_tech.structure_reactor.status." + statusKey(status));
    }

    private Component stageText(StateId state) {
        return Component.translatable(
                "screen.dimension_tech.structure_reactor.stage." + state.name().toLowerCase());
    }

    private static String statusKey(StructureReactorCycle.Status status) {
        return switch (status) {
            case IDLE -> "idle";
            case RUNNING -> "running";
            case REFINING -> "refining";
            case READY_TO_COMMIT -> "ready";
        };
    }

    static int sequenceColor(int index, int currentIndex, int statusOrdinal) {
        if (statusOrdinal != StructureReactorCycle.Status.RUNNING.ordinal() || currentIndex < 0)
            return SEQUENCE_PREVIOUS_COLOR;
        if (index < currentIndex) return SEQUENCE_PREVIOUS_COLOR;
        if (index == currentIndex) return SEQUENCE_CURRENT_COLOR;
        return SEQUENCE_NEXT_COLOR;
    }

    static int progressColor(StructureReactorCycle.Status status, int ticks) {
        if (status == StructureReactorCycle.Status.RUNNING && ticks >= TICKS)
            return PROGRESS_TIMEOUT_COLOR;
        if (status == StructureReactorCycle.Status.RUNNING
                && ticks >= StructureReactorCycle.REWARD_START_TICK
                && ticks <= StructureReactorCycle.REWARD_END_TICK) return PROGRESS_REWARD_COLOR;
        return SEQUENCE_NEXT_COLOR;
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(TICKS, ticks));
    }

    private static StructureReactorCycle.Status statusValue(int ordinal) {
        StructureReactorCycle.Status[] values = StructureReactorCycle.Status.values();
        return ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : StructureReactorCycle.Status.IDLE;
    }

    private record DetailLine(Component text, int color, boolean highlight) {
        static DetailLine header(Component text) {
            return new DetailLine(text, INK, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        if (StructureReactorLayout.INPUT_TANK.contains(localX, localY)) {
            renderFluidTooltip(g, true, mouseX, mouseY);
        } else if (StructureReactorLayout.OUTPUT_TANK.contains(localX, localY)) {
            renderFluidTooltip(g, false, mouseX, mouseY);
        } else if (StructureReactorLayout.FRAGMENT_SLOT.contains(localX, localY)) {
            renderFragmentTooltip(g, mouseX, mouseY);
        } else if (StructureReactorLayout.OPERATION_SLOT.contains(localX, localY)) {
            renderOperationTooltip(g, mouseX, mouseY);
        } else if (hoveredSlot != null && hoveredSlot.hasItem()) {
            g.renderTooltip(font, hoveredSlot.getItem(), mouseX, mouseY);
        }
    }

    private void pressButton(int buttonId) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    private void renderFluidTooltip(GuiGraphics g, boolean input, int mouseX, int mouseY) {
        Fluid current = input ? menu.inputFluid() : menu.outputFluid();
        ReactorTooltipSnapshot snapshot = menu.tooltipSnapshot();
        int amount = input ? menu.inputAmount() : menu.outputAmount();
        int capacity = input ? menu.inputCapacity() : menu.outputCapacity();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(input ? KEY_TOOLTIP_INPUT : KEY_TOOLTIP_OUTPUT));
        lines.add(Component.translatable(KEY_TOOLTIP_FLUID, fluidName(current)));
        lines.add(Component.translatable(KEY_TOOLTIP_AMOUNT, amount, capacity));
        if (input) {
            lines.add(
                    Component.translatable(
                            "screen.dimension_tech.structure_reactor.tooltip.required",
                            snapshot.inputRequiredAmount()));
            lines.add(
                    Component.translatable(
                            "screen.dimension_tech.structure_reactor.fluid_lock."
                                    + (menu.inputFluidLocked() ? "locked" : "unlocked")));
        } else {
            lines.add(
                    Component.translatable(
                            KEY_TOOLTIP_EXPECTED,
                            fluidName(snapshot.expectedOutputFluid()),
                            snapshot.outputExpectedAmount()));
            lines.add(Component.translatable(KEY_TOOLTIP_FREE, Math.max(0, capacity - amount)));
        }
        if (amount > 0) lines.add(Component.translatable(KEY_FLUID_CLEAR_HINT));
        g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderFragmentTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ItemStack current = menu.getSlot(StructureReactorBlockEntity.FRAGMENT_SLOT).getItem();
        ReactorTooltipSnapshot snapshot = menu.tooltipSnapshot();
        List<Component> lines = itemTooltip(current);
        lines.add(Component.translatable(KEY_TOOLTIP_FRAGMENT));
        addCandidates(lines, snapshot.fragmentCandidates(), snapshot.fragmentCandidateTotal());
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.structure_reactor.tooltip.required_count",
                        snapshot.fragmentRequiredCount()));
        g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderOperationTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ItemStack current = menu.getSlot(StructureReactorBlockEntity.OPERATION_SLOT).getItem();
        ReactorTooltipSnapshot snapshot = menu.tooltipSnapshot();
        List<Component> lines = itemTooltip(current);
        int step = snapshot.operationStepCount() == 0 ? 0 : snapshot.operationStepIndex() + 1;
        lines.add(Component.translatable(KEY_TOOLTIP_OPERATION));
        lines.add(Component.translatable(KEY_TOOLTIP_STEP, step, snapshot.operationStepCount()));
        if (menu.currentState() != null)
            lines.add(Component.translatable(KEY_TOOLTIP_STATE, menu.currentState().name()));
        addCandidates(lines, snapshot.operationCandidates(), snapshot.operationCandidateTotal());
        lines.add(
                Component.translatable(
                        KEY_TOOLTIP_CURRENT,
                        current.isEmpty()
                                ? Component.translatable(KEY_TOOLTIP_EMPTY_ITEM)
                                : current.getHoverName()));
        lines.add(Component.translatable(KEY_TOOLTIP_TIMEOUT, TICKS));
        g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private List<Component> itemTooltip(ItemStack stack) {
        if (stack.isEmpty())
            return new ArrayList<>(List.of(Component.translatable(KEY_TOOLTIP_EMPTY_ITEM)));
        return new ArrayList<>(getTooltipFromItem(minecraft, stack));
    }

    private void addCandidates(List<Component> lines, List<ItemStack> candidates, int total) {
        for (ItemStack candidate : candidates)
            lines.add(Component.translatable(KEY_TOOLTIP_NEEDS, candidate.getHoverName()));
        if (total > candidates.size())
            lines.add(Component.translatable(KEY_TOOLTIP_MORE, total - candidates.size()));
    }

    private Component fluidName(Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY
                ? Component.translatable(KEY_TOOLTIP_EMPTY)
                : Component.translatable(fluid.getFluidType().getDescriptionId());
    }
}
