package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.entity.MythicCrucibleBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleMenu;
import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleTooltipSnapshot;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.StateId;
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

/** Texture-backed Mythic Crucible screen. */
public final class MythicCrucibleScreen extends AbstractContainerScreen<MythicCrucibleMenu> {
    private static final int TICKS = MythicCrucibleCycle.STATE_TIMEOUT_TICKS;
    private static final int FLUID_FALLBACK = 0xFF7F879F;
    private static final int TITLE_LABEL_X = 7;
    private static final int TITLE_LABEL_Y = 6;
    private static final int INVENTORY_LABEL_X = MythicCrucibleLayout.PLAYER_INVENTORY.x();
    private static final int INVENTORY_LABEL_Y = MythicCrucibleLayout.PLAYER_INVENTORY.y() - 10;
    private static final int LABEL_COLOR = 0xFF413F54;
    static final int SEQUENCE_PREVIOUS_COLOR = 0xFF777783;
    static final int SEQUENCE_CURRENT_COLOR = 0xFFB66CFF;
    static final int SEQUENCE_NEXT_COLOR = 0xFFFFFFFF;
    static final int PROGRESS_REWARD_COLOR = 0xFFFFA33A;
    static final int PROGRESS_TIMEOUT_COLOR = 0xFFFF4A4A;
    private static final int STATUS_X = MythicCrucibleLayout.STATUS_DISPLAY.x() + 2;
    private static final int STATUS_Y = MythicCrucibleLayout.STATUS_DISPLAY.y() + 2;
    private static final int STATUS_WIDTH = MythicCrucibleLayout.STATUS_DISPLAY.width() - 4;
    private static final int STATUS_LINE_HEIGHT = 9;
    private static final String KEY_TITLE = "screen.dimension_tech.mythic_crucible.title";
    private static final String KEY_INVENTORY_LABEL =
            "screen.dimension_tech.mythic_crucible.inventory_label";
    private static final String KEY_TOOLTIP_INPUT =
            "screen.dimension_tech.mythic_crucible.tooltip.input";
    private static final String KEY_TOOLTIP_OUTPUT =
            "screen.dimension_tech.mythic_crucible.tooltip.output";
    private static final String KEY_TOOLTIP_EMPTY =
            "screen.dimension_tech.mythic_crucible.tooltip.empty";
    private static final String KEY_TOOLTIP_FLUID =
            "screen.dimension_tech.mythic_crucible.tooltip.fluid";
    private static final String KEY_TOOLTIP_AMOUNT =
            "screen.dimension_tech.mythic_crucible.tooltip.amount";
    private static final String KEY_TOOLTIP_EXPECTED =
            "screen.dimension_tech.mythic_crucible.tooltip.expected";
    private static final String KEY_TOOLTIP_FREE =
            "screen.dimension_tech.mythic_crucible.tooltip.free";
    private static final String KEY_TOOLTIP_FRAGMENT =
            "screen.dimension_tech.mythic_crucible.tooltip.fragment";
    private static final String KEY_TOOLTIP_OPERATION =
            "screen.dimension_tech.mythic_crucible.tooltip.operation";
    private static final String KEY_TOOLTIP_STEP =
            "screen.dimension_tech.mythic_crucible.tooltip.step";
    private static final String KEY_TOOLTIP_STATE =
            "screen.dimension_tech.mythic_crucible.tooltip.state";
    private static final String KEY_TOOLTIP_CURRENT =
            "screen.dimension_tech.mythic_crucible.tooltip.current";
    private static final String KEY_TOOLTIP_EMPTY_ITEM =
            "screen.dimension_tech.mythic_crucible.tooltip.empty_item";
    private static final String KEY_TOOLTIP_NEEDS =
            "screen.dimension_tech.mythic_crucible.tooltip.needs";
    private static final String KEY_TOOLTIP_MORE =
            "screen.dimension_tech.mythic_crucible.tooltip.more";
    private static final String KEY_TOOLTIP_TIMEOUT =
            "screen.dimension_tech.mythic_crucible.tooltip.timeout";
    private static final String KEY_FLUID_CLEAR_HINT =
            "screen.dimension_tech.mythic_crucible.fluid_clear_hint";
    private static final String KEY_STATUS_CURRENT =
            "screen.dimension_tech.mythic_crucible.status_line.current";
    private static final String KEY_STATUS_SEQUENCE =
            "screen.dimension_tech.mythic_crucible.status_line.sequence";
    private static final String KEY_STATUS_PROGRESS =
            "screen.dimension_tech.mythic_crucible.status_line.progress";
    private static final String KEY_STATUS_REFINING_PROGRESS =
            "screen.dimension_tech.mythic_crucible.status_line.refining_progress";
    private static final String KEY_STATUS_NEEDS =
            "screen.dimension_tech.mythic_crucible.status_line.needs";
    private static final String KEY_STATUS_PREVIOUS =
            "screen.dimension_tech.mythic_crucible.status_line.previous";
    private static final String KEY_STATUS_NO_RECIPE =
            "screen.dimension_tech.mythic_crucible.status_line.no_recipe";
    private static final String KEY_STATUS_NO_REQUIREMENT =
            "screen.dimension_tech.mythic_crucible.status_line.no_requirement";
    private static final String KEY_STATUS_CHANGES =
            "screen.dimension_tech.mythic_crucible.status_line.changes";

    public MythicCrucibleScreen(MythicCrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MythicCrucibleLayout.WIDTH;
        imageHeight = MythicCrucibleLayout.HEIGHT;
        inventoryLabelY = Integer.MIN_VALUE;
        titleLabelY = Integer.MIN_VALUE;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, MythicMinerTheme.BACKDROP);
        g.blit(
                MythicCrucibleLayout.TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                0,
                MythicCrucibleLayout.WIDTH,
                MythicCrucibleLayout.HEIGHT,
                MythicCrucibleLayout.TEXTURE_WIDTH,
                MythicCrucibleLayout.TEXTURE_HEIGHT);
        drawFluid(
                g,
                MythicCrucibleLayout.INPUT_TANK,
                menu.inputFluid(),
                menu.inputAmount(),
                menu.inputCapacity());
        drawFluid(
                g,
                MythicCrucibleLayout.OUTPUT_TANK,
                menu.outputFluid(),
                menu.outputAmount(),
                menu.outputCapacity());
        drawProgressOverlay(g);
    }

    private void drawFluid(GuiGraphics g, GuiRect tank, Fluid fluid, int amount, int capacity) {
        int innerX = leftPos + MythicCrucibleLayout.innerX(tank);
        int innerY = topPos + MythicCrucibleLayout.innerY(tank);
        int innerWidth = MythicCrucibleLayout.innerWidth(tank);
        int innerHeight = MythicCrucibleLayout.innerHeight(tank);
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
        int scaleX = leftPos + tank.x() + tank.width() - MythicCrucibleLayout.FLUID_SCALE_WIDTH - 1;
        int scaleY =
                topPos + tank.y() + (tank.height() - MythicCrucibleLayout.FLUID_SCALE_HEIGHT) / 2;
        g.blit(
                MythicCrucibleLayout.TEXTURE,
                scaleX,
                scaleY,
                0,
                MythicCrucibleLayout.FLUID_SCALE_U,
                MythicCrucibleLayout.FLUID_SCALE_V,
                MythicCrucibleLayout.FLUID_SCALE_WIDTH,
                MythicCrucibleLayout.FLUID_SCALE_HEIGHT,
                MythicCrucibleLayout.TEXTURE_WIDTH,
                MythicCrucibleLayout.TEXTURE_HEIGHT);
    }

    static int progressPixels(long value, long max, int pixels) {
        if (max <= 0 || pixels <= 0) return 0;
        return (int) Math.max(0, Math.min(pixels, value * pixels / max));
    }

    private void drawProgressOverlay(GuiGraphics g) {
        int status = menu.status();
        long value;
        long maximum;
        if (status == MythicCrucibleCycle.Status.REFINING.ordinal()) {
            value = menu.refiningTicks();
            maximum = menu.resultTimeTicks();
        } else if (status == MythicCrucibleCycle.Status.RUNNING.ordinal()) {
            value = menu.stateTicks();
            maximum = TICKS;
        } else if (status == MythicCrucibleCycle.Status.READY_TO_COMMIT.ordinal()) {
            value = 1;
            maximum = 1;
        } else {
            value = 0;
            maximum = 1;
        }
        int covered = progressPixels(value, maximum, MythicCrucibleLayout.PROGRESS_BAR.height());
        if (covered <= 0) return;
        GuiRect bar = MythicCrucibleLayout.PROGRESS_BAR;
        g.blit(
                MythicCrucibleLayout.TEXTURE,
                leftPos + bar.x(),
                topPos + bar.y(),
                0,
                MythicCrucibleLayout.PROGRESS_OVERLAY_U,
                MythicCrucibleLayout.PROGRESS_OVERLAY_V,
                MythicCrucibleLayout.PROGRESS_OVERLAY_WIDTH,
                covered,
                MythicCrucibleLayout.TEXTURE_WIDTH,
                MythicCrucibleLayout.TEXTURE_HEIGHT);
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
        renderStatus(g);
    }

    private void renderStatus(GuiGraphics g) {
        MythicCrucibleCycle.Status status = statusValue(menu.status());
        drawFitted(
                g,
                Component.translatable(
                        KEY_STATUS_CURRENT, statusText(status), progressValueText(status)),
                STATUS_X,
                STATUS_Y,
                SEQUENCE_NEXT_COLOR);
        renderSequence(g, status);
        drawFitted(
                g,
                Component.translatable(KEY_STATUS_NEEDS, operationRequirement()),
                STATUS_X,
                STATUS_Y + STATUS_LINE_HEIGHT * 2,
                SEQUENCE_NEXT_COLOR);
        drawFitted(
                g,
                recipeChangesText(),
                STATUS_X,
                STATUS_Y + STATUS_LINE_HEIGHT * 3,
                SEQUENCE_NEXT_COLOR);
        drawFitted(
                g,
                previousStatusText(),
                STATUS_X,
                STATUS_Y + STATUS_LINE_HEIGHT * 4,
                SEQUENCE_NEXT_COLOR);
    }

    private void renderSequence(GuiGraphics g, MythicCrucibleCycle.Status status) {
        List<ColoredText> parts = new ArrayList<>();
        int length = Math.max(0, Math.min(menu.sequenceLength(), 32));
        int currentIndex = status == MythicCrucibleCycle.Status.RUNNING ? menu.stateIndex() : -1;
        for (int index = 0; index < length; index++) {
            StateId state = menu.sequenceState(index);
            if (state == null) continue;
            if (index > 0)
                parts.add(new ColoredText(Component.literal(" → "), SEQUENCE_NEXT_COLOR));
            parts.add(
                    new ColoredText(
                            stageText(state),
                            sequenceColor(index, currentIndex, status.ordinal())));
        }
        if (length == 0)
            parts.add(
                    new ColoredText(
                            Component.translatable(KEY_STATUS_NO_RECIPE), SEQUENCE_NEXT_COLOR));
        drawFittedSequence(g, parts, STATUS_X, STATUS_Y + STATUS_LINE_HEIGHT);
    }

    private Component progressText(MythicCrucibleCycle.Status status) {
        if (status == MythicCrucibleCycle.Status.REFINING) {
            return Component.translatable(
                    KEY_STATUS_REFINING_PROGRESS, Math.max(0, menu.refiningTicks()));
        }
        int ticks =
                status == MythicCrucibleCycle.Status.RUNNING
                        ? clampTicks(menu.stateTicks())
                        : status == MythicCrucibleCycle.Status.READY_TO_COMMIT ? TICKS : 0;
        return Component.translatable(KEY_STATUS_PROGRESS, ticks, TICKS);
    }

    private Component progressValueText(MythicCrucibleCycle.Status status) {
        if (status == MythicCrucibleCycle.Status.REFINING)
            return Component.translatable(
                    "screen.dimension_tech.mythic_crucible.status_line.refining_value",
                    Math.max(0, menu.refiningTicks()));
        int ticks =
                status == MythicCrucibleCycle.Status.RUNNING
                        ? clampTicks(menu.stateTicks())
                        : status == MythicCrucibleCycle.Status.READY_TO_COMMIT ? TICKS : 0;
        return Component.translatable(
                "screen.dimension_tech.mythic_crucible.status_line.progress_value", ticks, TICKS);
    }

    private Component recipeChangesText() {
        int outputChange = menu.outputBonusBp() - menu.outputPenaltyBp();
        int consumptionChange = menu.fluidReductionBp() - menu.fluidPenaltyBp();
        return Component.translatable(
                KEY_STATUS_CHANGES,
                signedPercent(outputChange),
                signedPercent(-consumptionChange),
                Math.max(0, menu.timeReduction()),
                Math.max(0, menu.timePenalty()),
                Math.max(0, menu.extraFragments()));
    }

    static String signedPercent(int basisPoints) {
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

    private Component operationRequirement() {
        CrucibleTooltipSnapshot snapshot = menu.tooltipSnapshot();
        if (snapshot.operationCandidates().isEmpty())
            return Component.translatable(KEY_STATUS_NO_REQUIREMENT);
        StringJoiner names = new StringJoiner(", ");
        for (ItemStack candidate : snapshot.operationCandidates())
            names.add(candidate.getHoverName().getString());
        if (snapshot.operationCandidateTotal() > snapshot.operationCandidates().size())
            names.add(
                    "+"
                            + (snapshot.operationCandidateTotal()
                                    - snapshot.operationCandidates().size()));
        return Component.literal(names.toString());
    }

    private Component previousStatusText() {
        MythicCrucibleCycle.Resolution resolution = menu.lastResolution();
        if (resolution == null || resolution == MythicCrucibleCycle.Resolution.NONE)
            return Component.translatable(
                    KEY_STATUS_PREVIOUS,
                    Component.translatable(
                            "screen.dimension_tech.mythic_crucible.status_line.previous.none"));
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
                                    "screen.dimension_tech.mythic_crucible.status_line.previous.normal");
                    case PHASE_IDLE ->
                            Component.translatable(
                                    "screen.dimension_tech.mythic_crucible.status_line.previous.penalty.phase_idle",
                                    penaltyTicks(
                                            MythicCrucibleCycle.WRONG_STATE_TIME_PENALTY_TICKS));
                    case BRANCH_CONFLICT ->
                            Component.translatable(
                                    "screen.dimension_tech.mythic_crucible.status_line.previous.penalty.branch",
                                    penaltyTicks(
                                            MythicCrucibleCycle
                                                    .BRANCH_CONFLICT_TIME_PENALTY_TICKS));
                    case RECURSION_OVERFLOW ->
                            Component.translatable(
                                    "screen.dimension_tech.mythic_crucible.status_line.previous.penalty.recurse",
                                    MythicCrucibleCycle.RECURSION_OVERFLOW_FLUID_PENALTY_BP / 100);
                    case EARLY_CONVERGE ->
                            Component.translatable(
                                    "screen.dimension_tech.mythic_crucible.status_line.previous.penalty.converge",
                                    MythicCrucibleCycle.EARLY_CONVERGE_OUTPUT_PENALTY_BP_PER_DEPTH
                                            / 100);
                    case STABILIZE_FAILURE ->
                            Component.translatable(
                                    "screen.dimension_tech.mythic_crucible.status_line.previous.penalty.stabilize",
                                    Math.max(0, menu.extraFragments()));
                    default ->
                            Component.translatable(
                                    "screen.dimension_tech.mythic_crucible.status_line.previous.none");
                };
        return Component.translatable(
                KEY_STATUS_PREVIOUS,
                Component.translatable(
                        "screen.dimension_tech.mythic_crucible.status_line.previous." + detailKey,
                        step,
                        detail));
    }

    private Component rewardDetail(StateId state) {
        if (state == null)
            return Component.translatable(
                    "screen.dimension_tech.mythic_crucible.status_line.previous.reward");
        return switch (state) {
            case BRANCH ->
                    Component.translatable(
                            "screen.dimension_tech.mythic_crucible.status_line.previous.reward.branch",
                            Math.max(0, menu.timeReduction()));
            case RECURSE ->
                    Component.translatable(
                            "screen.dimension_tech.mythic_crucible.status_line.previous.reward.recurse",
                            Math.max(0, menu.fluidReductionBp()) / 100);
            case CONVERGE ->
                    Component.translatable(
                            "screen.dimension_tech.mythic_crucible.status_line.previous.reward.converge",
                            Math.max(0, menu.outputBonusBp()) / 100);
            case STABILIZE ->
                    Component.translatable(
                            "screen.dimension_tech.mythic_crucible.status_line.previous.reward.stabilize");
        };
    }

    private int penaltyTicks(int fallback) {
        int total = Math.max(0, menu.timePenalty());
        return total == 0 ? fallback : total;
    }

    private Component statusText(MythicCrucibleCycle.Status status) {
        return Component.translatable(
                "screen.dimension_tech.mythic_crucible.status." + statusKey(status));
    }

    private Component stageText(StateId state) {
        return Component.translatable(
                "screen.dimension_tech.mythic_crucible.stage." + state.name().toLowerCase());
    }

    private static String statusKey(MythicCrucibleCycle.Status status) {
        return switch (status) {
            case IDLE -> "idle";
            case RUNNING -> "running";
            case REFINING -> "refining";
            case READY_TO_COMMIT -> "ready";
        };
    }

    static int sequenceColor(int index, int currentIndex, int statusOrdinal) {
        if (statusOrdinal != MythicCrucibleCycle.Status.RUNNING.ordinal() || currentIndex < 0)
            return SEQUENCE_PREVIOUS_COLOR;
        if (index < currentIndex) return SEQUENCE_PREVIOUS_COLOR;
        if (index == currentIndex) return SEQUENCE_CURRENT_COLOR;
        return SEQUENCE_NEXT_COLOR;
    }

    static int progressColor(MythicCrucibleCycle.Status status, int ticks) {
        if (status == MythicCrucibleCycle.Status.RUNNING && ticks >= TICKS)
            return PROGRESS_TIMEOUT_COLOR;
        if (status == MythicCrucibleCycle.Status.RUNNING
                && ticks >= MythicCrucibleCycle.REWARD_START_TICK
                && ticks <= MythicCrucibleCycle.REWARD_END_TICK) return PROGRESS_REWARD_COLOR;
        return SEQUENCE_NEXT_COLOR;
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(TICKS, ticks));
    }

    private static MythicCrucibleCycle.Status statusValue(int ordinal) {
        MythicCrucibleCycle.Status[] values = MythicCrucibleCycle.Status.values();
        return ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : MythicCrucibleCycle.Status.IDLE;
    }

    private void drawFitted(GuiGraphics g, Component text, int x, int y, int color) {
        drawFitted(g, text, x, y, STATUS_WIDTH, color);
    }

    private void drawFitted(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            g.drawString(font, text, x, y, color, false);
            return;
        }
        float scale = Math.min(1.0F, (float) maxWidth / Math.max(1, textWidth));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawFittedSequence(GuiGraphics g, List<ColoredText> parts, int x, int y) {
        int width = 0;
        for (ColoredText part : parts) width += font.width(part.text());
        float scale = Math.min(1.0F, (float) STATUS_WIDTH / Math.max(1, width));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        int cursor = 0;
        for (ColoredText part : parts) {
            g.drawString(font, part.text(), cursor, 0, part.color(), false);
            cursor += font.width(part.text());
        }
        g.pose().popPose();
    }

    private record ColoredText(Component text, int color) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        if (MythicCrucibleLayout.INPUT_TANK.contains(localX, localY)) {
            renderFluidTooltip(g, true, mouseX, mouseY);
        } else if (MythicCrucibleLayout.OUTPUT_TANK.contains(localX, localY)) {
            renderFluidTooltip(g, false, mouseX, mouseY);
        } else if (MythicCrucibleLayout.FRAGMENT_SLOT.contains(localX, localY)) {
            renderFragmentTooltip(g, mouseX, mouseY);
        } else if (MythicCrucibleLayout.OPERATION_SLOT.contains(localX, localY)) {
            renderOperationTooltip(g, mouseX, mouseY);
        } else if (hoveredSlot != null && hoveredSlot.hasItem()) {
            g.renderTooltip(font, hoveredSlot.getItem(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (int) mouseX - leftPos;
            int y = (int) mouseY - topPos;
            boolean clearing = hasShiftDown();
            if (MythicCrucibleLayout.INPUT_TANK.contains(x, y)) {
                pressButton(
                        clearing
                                ? MythicCrucibleBlockEntity.BUTTON_CLEAR_INPUT_TANK
                                : MythicCrucibleBlockEntity.BUTTON_TOGGLE_INPUT_LOCK);
                return true;
            }
            if (clearing && MythicCrucibleLayout.OUTPUT_TANK.contains(x, y)) {
                pressButton(MythicCrucibleBlockEntity.BUTTON_CLEAR_OUTPUT_TANK);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void pressButton(int buttonId) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    private void renderFluidTooltip(GuiGraphics g, boolean input, int mouseX, int mouseY) {
        Fluid current = input ? menu.inputFluid() : menu.outputFluid();
        CrucibleTooltipSnapshot snapshot = menu.tooltipSnapshot();
        int amount = input ? menu.inputAmount() : menu.outputAmount();
        int capacity = input ? menu.inputCapacity() : menu.outputCapacity();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(input ? KEY_TOOLTIP_INPUT : KEY_TOOLTIP_OUTPUT));
        lines.add(Component.translatable(KEY_TOOLTIP_FLUID, fluidName(current)));
        lines.add(Component.translatable(KEY_TOOLTIP_AMOUNT, amount, capacity));
        if (input) {
            lines.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_crucible.tooltip.required",
                            snapshot.inputRequiredAmount()));
            lines.add(
                    Component.translatable(
                            "screen.dimension_tech.mythic_crucible.fluid_lock."
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
        ItemStack current = menu.getSlot(MythicCrucibleBlockEntity.FRAGMENT_SLOT).getItem();
        CrucibleTooltipSnapshot snapshot = menu.tooltipSnapshot();
        List<Component> lines = itemTooltip(current);
        lines.add(Component.translatable(KEY_TOOLTIP_FRAGMENT));
        addCandidates(lines, snapshot.fragmentCandidates(), snapshot.fragmentCandidateTotal());
        lines.add(
                Component.translatable(
                        "screen.dimension_tech.mythic_crucible.tooltip.required_count",
                        snapshot.fragmentRequiredCount()));
        g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderOperationTooltip(GuiGraphics g, int mouseX, int mouseY) {
        ItemStack current = menu.getSlot(MythicCrucibleBlockEntity.OPERATION_SLOT).getItem();
        CrucibleTooltipSnapshot snapshot = menu.tooltipSnapshot();
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
