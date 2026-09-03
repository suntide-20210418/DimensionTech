package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.MythicMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerLayout;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Owns the complete attributes-page rendering and interaction behavior. */
final class MythicMinerAttributesPage {
    private static final int X = 28;
    private static final int Y = 52;
    private static final int LIST_TOP = Y + 108;
    private static final int VIEWPORT_Y = LIST_TOP + 20;
    private static final MythicMinerUpgradeBlock.Type[] TYPES = {
        MythicMinerUpgradeBlock.Type.EFFICIENCY,
        MythicMinerUpgradeBlock.Type.ENERGY,
        MythicMinerUpgradeBlock.Type.PARALLEL,
        MythicMinerUpgradeBlock.Type.LUCK,
        MythicMinerUpgradeBlock.Type.AGGREGATE
    };

    private MythicMinerAttributesPage() {}

    static void render(MythicMinerScreenContext c, GuiGraphics g) {
        int width = c.imageWidth() - 56;
        int height = c.playerInventoryY() + 70 - Y;
        MythicMinerTheme.panel(g, X, Y, width, height, MythicMinerTheme.SUCCESS);
        int cellWidth = width / 2 - 12;
        drawCell(
                c,
                g,
                X + 8,
                Y + 12,
                cellWidth,
                "efficiency",
                c.menu().getEfficiencyUpgradeCount() + c.menu().getAggregateUpgradeCount(),
                decimal(c.menu().getEfficiencyHundredths()),
                c.menu().getEfficiencyBonusHundredths(),
                false,
                MythicMinerScreen.CYAN);
        drawCell(
                c,
                g,
                X + width / 2 + 4,
                Y + 12,
                cellWidth,
                "capacity",
                c.menu().getEnergyUpgradeCount() + c.menu().getAggregateUpgradeCount(),
                compact(c.menu().getEnergyCapacity()) + " FE",
                c.menu().getCapacityBonusHundredths(),
                false,
                MythicMinerScreen.AMBER);
        drawCell(
                c,
                g,
                X + 8,
                Y + 41,
                cellWidth,
                "consumption",
                c.menu().getEnergyUpgradeCount() + c.menu().getAggregateUpgradeCount(),
                compact(c.menu().getEffectiveEnergyConsumption()) + " FE/t",
                c.menu().getConsumptionReductionHundredths(),
                true,
                MythicMinerScreen.AMBER);
        drawCell(
                c,
                g,
                X + width / 2 + 4,
                Y + 41,
                cellWidth,
                "parallel",
                c.menu().getParallelUpgradeCount() + c.menu().getAggregateUpgradeCount(),
                compact(c.menu().getBaseParallel()),
                c.menu().getParallelBonusHundredths(),
                false,
                MythicMinerScreen.CYAN);
        drawCell(
                c,
                g,
                X + 8,
                Y + 70,
                cellWidth,
                "luck",
                c.menu().getLuckUpgradeCount() + c.menu().getAggregateUpgradeCount(),
                decimal(c.menu().getLuckHundredths()),
                c.menu().getLuckBonusHundredths(),
                false,
                MythicMinerScreen.CYAN);
        drawSummary(c, g, X + width / 2 + 4, Y + 70, cellWidth);

        g.fill(X + 8, LIST_TOP, X + width - 8, LIST_TOP + 1, MythicMinerScreen.RULE);
        g.renderItem(new ItemStack(Items.COMPARATOR), X + 8, LIST_TOP + 5);
        g.drawString(
                c.font(),
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.installed"),
                X + 28,
                LIST_TOP + 9,
                MythicMinerScreen.MUTED,
                false);
        int viewportHeight = Math.max(1, height - (VIEWPORT_Y - Y) - 8);
        c.attributeContentHeight(Math.max(viewportHeight, contentHeight(c)));
        int maxScroll = Math.max(0, c.attributeContentHeight() - viewportHeight);
        c.attributeScroll(Math.min(c.attributeScroll(), maxScroll));
        g.fill(
                X + 8,
                VIEWPORT_Y,
                X + width - 8,
                VIEWPORT_Y + viewportHeight,
                MythicMinerScreen.PANEL_INSET);
        MythicMinerLayout.ScissorBounds scissor =
                MythicMinerLayout.scaleToScreen(
                        c.leftPos() + X + 8,
                        c.topPos() + VIEWPORT_Y,
                        width - 16,
                        viewportHeight,
                        c.uiScale());
        g.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
        int rowY = VIEWPORT_Y - c.attributeScroll();
        for (MythicMinerUpgradeBlock.Type type : TYPES) {
            for (int tier = 1; tier <= 6; tier++) {
                int count = c.menu().getUpgradeCount(type, tier);
                if (count <= 0) continue;
                drawUpgradeRow(c, g, X + 8, rowY, width - 16, type, tier, count);
                rowY += rowHeight(c, type, tier);
            }
        }
        g.disableScissor();
        if (c.attributeContentHeight() > viewportHeight) {
            int thumbHeight =
                    Math.max(12, viewportHeight * viewportHeight / c.attributeContentHeight());
            int thumbY =
                    VIEWPORT_Y
                            + (viewportHeight - thumbHeight)
                                    * c.attributeScroll()
                                    / Math.max(1, maxScroll);
            g.fill(
                    X + width - 11,
                    VIEWPORT_Y,
                    X + width - 10,
                    VIEWPORT_Y + viewportHeight,
                    MythicMinerScreen.RULE);
            g.fill(
                    X + width - 12,
                    thumbY,
                    X + width - 9,
                    thumbY + thumbHeight,
                    MythicMinerScreen.GREEN);
        }
    }

    static boolean mouseClicked(MythicMinerScreenContext c, double x, double y, int button) {
        if (button == 0) {
            int key = rowAt(c, x, y);
            if (key >= 0) {
                c.toggleUpgradeRow(key);
                c.attributeScroll(0);
            }
        }
        return true;
    }

    static boolean mouseScrolled(
            MythicMinerScreenContext c, double logicalX, double logicalY, double delta) {
        int viewportHeight = Math.max(1, c.playerInventoryY() + 70 - VIEWPORT_Y - 8);
        if (MythicMinerScreen.inside(
                logicalX,
                logicalY,
                c.leftPos() + X,
                c.topPos() + VIEWPORT_Y,
                c.imageWidth() - 56,
                viewportHeight)) {
            int maxScroll = Math.max(0, c.attributeContentHeight() - viewportHeight);
            c.attributeScroll(
                    Math.max(
                            0,
                            Math.min(
                                    maxScroll,
                                    c.attributeScroll()
                                            - (int) Math.signum(delta)
                                                    * MythicMinerScreen.MARKER_INFO_ROW_HEIGHT)));
        }
        return true;
    }

    static void renderTooltip(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int logicalX,
            int logicalY,
            int screenX,
            int screenY) {
        int x = c.leftPos() + X + 8;
        int y = c.topPos() + Y + 12;
        int width = c.imageWidth() - 56;
        int cellWidth = width / 2 - 12;
        int rightX = c.leftPos() + X + width / 2 + 4;
        int aggregate = c.menu().getAggregateUpgradeCount();
        List<Component> tooltip = null;
        if (inside(logicalX, logicalY, x, y, cellWidth, 27))
            tooltip =
                    tooltip(
                            "efficiency",
                            decimal(c.menu().getEfficiencyHundredths()),
                            c.menu().getEfficiencyBonusHundredths(),
                            false,
                            c.menu().getEfficiencyUpgradeCount(),
                            aggregate);
        else if (inside(logicalX, logicalY, rightX, y, cellWidth, 27))
            tooltip =
                    tooltip(
                            "capacity",
                            c.menu().getEnergyCapacity(),
                            c.menu().getCapacityBonusHundredths(),
                            false,
                            c.menu().getEnergyUpgradeCount(),
                            aggregate);
        else if (inside(logicalX, logicalY, x, y + 29, cellWidth, 27))
            tooltip =
                    tooltip(
                            "consumption",
                            c.menu().getEffectiveEnergyConsumption(),
                            c.menu().getConsumptionReductionHundredths(),
                            true,
                            c.menu().getEnergyUpgradeCount(),
                            aggregate);
        else if (inside(logicalX, logicalY, rightX, y + 29, cellWidth, 27))
            tooltip =
                    tooltip(
                            "parallel",
                            c.menu().getBaseParallel(),
                            c.menu().getParallelBonusHundredths(),
                            false,
                            c.menu().getParallelUpgradeCount(),
                            aggregate);
        else if (inside(logicalX, logicalY, x, y + 58, cellWidth, 27))
            tooltip =
                    tooltip(
                            "luck",
                            decimal(c.menu().getLuckHundredths()),
                            c.menu().getLuckBonusHundredths(),
                            false,
                            c.menu().getLuckUpgradeCount(),
                            aggregate);
        else if (inside(logicalX, logicalY, rightX, y + 58, cellWidth, 27))
            tooltip =
                    List.of(
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.attribute.total_count",
                                    c.menu().getTotalUpgradeCount()),
                            Component.translatable(
                                    "screen.dimension_tech.mythic_miner.attribute.upgrade_breakdown",
                                    c.menu().getTotalUpgradeCount() - aggregate,
                                    aggregate));
        if (tooltip != null) g.renderTooltip(c.font(), tooltip, Optional.empty(), screenX, screenY);
    }

    static void resetScroll(MythicMinerScreenContext c) {
        c.attributeScroll(0);
    }

    private static void drawCell(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            int width,
            String name,
            int count,
            String value,
            int bonus,
            boolean reduction,
            int accent) {
        g.fill(x, y, x + width, y + 27, MythicMinerScreen.PANEL_INSET);
        g.fill(x, y, x + 2, y + 27, accent);
        g.fill(x + width - 1, y, x + width, y + 27, MythicMinerScreen.RULE);
        g.drawString(
                c.font(),
                Component.translatable("screen.dimension_tech.mythic_miner.attribute." + name),
                x + 7,
                y + 4,
                MythicMinerScreen.MUTED,
                false);
        Component countText =
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.upgrade_count", count);
        g.drawString(
                c.font(),
                countText,
                x + width - 7 - c.font().width(countText),
                y + 4,
                accent,
                false);
        g.drawString(c.font(), value, x + 7, y + 15, MythicMinerScreen.TEXT, false);
        String bonusText = bonus(bonus, reduction);
        g.drawString(
                c.font(),
                bonusText,
                x + width - 7 - c.font().width(bonusText),
                y + 15,
                accent,
                false);
    }

    private static void drawSummary(
            MythicMinerScreenContext c, GuiGraphics g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 27, MythicMinerScreen.PANEL_INSET);
        g.fill(x, y, x + 2, y + 27, MythicMinerScreen.CYAN_DARK);
        g.fill(x + width - 1, y, x + width, y + 27, MythicMinerScreen.RULE);
        g.drawString(
                c.font(),
                Component.translatable("screen.dimension_tech.mythic_miner.attribute.upgrades"),
                x + 7,
                y + 4,
                MythicMinerScreen.MUTED,
                false);
        Component total =
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.total_short",
                        c.menu().getTotalUpgradeCount());
        g.drawString(
                c.font(),
                total,
                x + width - 7 - c.font().width(total),
                y + 4,
                MythicMinerScreen.CYAN,
                false);
        g.drawCenteredString(
                c.font(),
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.summary_counts",
                        c.menu().getTotalUpgradeCount() - c.menu().getAggregateUpgradeCount(),
                        c.menu().getAggregateUpgradeCount()),
                x + width / 2,
                y + 15,
                MythicMinerScreen.TEXT);
    }

    private static void drawUpgradeRow(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            int width,
            MythicMinerUpgradeBlock.Type type,
            int tier,
            int count) {
        Item icon =
                switch (type) {
                    case EFFICIENCY -> Items.REDSTONE;
                    case ENERGY -> Items.BEACON;
                    case PARALLEL -> Items.ARROW;
                    case LUCK -> Items.RABBIT_FOOT;
                    case AGGREGATE -> Items.NETHER_STAR;
                    default -> Items.STONE;
                };
        g.renderItem(new ItemStack(icon), x, y);
        String typeName = Component.translatable(upgradeTranslationKey(type, tier)).getString();
        boolean expanded = c.upgradeRowExpanded(key(type, tier));
        g.drawString(
                c.font(), typeName + "  x" + count, x + 20, y + 3, MythicMinerScreen.TEXT, false);
        g.drawString(
                c.font(),
                Component.literal(expanded ? "-" : "+"),
                x + width - 10,
                y + 4,
                expanded ? MythicMinerScreen.GREEN : MythicMinerScreen.MUTED,
                false);
        if (!expanded) {
            g.fill(x, y + 20, x + width, y + 21, MythicMinerScreen.RULE);
            return;
        }
        ModConfigs.MythicMinerUpgradeTierConfig cfg =
                type == MythicMinerUpgradeBlock.Type.AGGREGATE
                        ? ModConfigs.AGGREGATE_UPGRADE_TIERS[tier - 1]
                        : ModConfigs.UPGRADE_TIERS[tier - 1];
        int lineY = y + 23;
        if (type == MythicMinerUpgradeBlock.Type.EFFICIENCY
                || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            bonusLine(
                    c,
                    g,
                    x + 20,
                    lineY,
                    width - 24,
                    "efficiency",
                    cfg.efficiencyIncreasePercent(),
                    MythicMinerScreen.CYAN);
            lineY += 12;
        }
        if (type == MythicMinerUpgradeBlock.Type.ENERGY
                || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            bonusLine(
                    c,
                    g,
                    x + 20,
                    lineY,
                    width - 24,
                    "energy_capacity",
                    cfg.energyCapacityIncreasePercent(),
                    MythicMinerScreen.AMBER);
            lineY += 12;
            String energyKey =
                    type == MythicMinerUpgradeBlock.Type.AGGREGATE
                            ? "energy_consumption.additive"
                            : "energy_consumption.multiplicative";
            bonusLine(
                    c,
                    g,
                    x + 20,
                    lineY,
                    width - 24,
                    energyKey,
                    cfg.energyConsumptionReductionPercent(),
                    MythicMinerScreen.AMBER);
            lineY += 12;
        }
        if (type == MythicMinerUpgradeBlock.Type.PARALLEL
                || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            bonusLine(
                    c,
                    g,
                    x + 20,
                    lineY,
                    width - 24,
                    "parallel",
                    cfg.parallelIncreasePercent(),
                    MythicMinerScreen.CYAN);
            lineY += 12;
        }
        if (type == MythicMinerUpgradeBlock.Type.LUCK
                || type == MythicMinerUpgradeBlock.Type.AGGREGATE) {
            bonusLine(
                    c,
                    g,
                    x + 20,
                    lineY,
                    width - 24,
                    "luck",
                    cfg.luckIncreasePercent(),
                    MythicMinerScreen.CYAN);
            lineY += 12;
        }
        g.fill(x, lineY + 1, x + width, lineY + 2, MythicMinerScreen.RULE);
    }

    private static void bonusLine(
            MythicMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            int width,
            String suffix,
            double value,
            int color) {
        Component text =
                Component.translatable(
                        "tooltip.dimension_tech.mythic_miner.upgrade." + suffix,
                        configPercent(value));
        g.drawString(
                c.font(), c.font().plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private static int contentHeight(MythicMinerScreenContext c) {
        int height = 0;
        for (MythicMinerUpgradeBlock.Type type : TYPES)
            for (int tier = 1; tier <= 6; tier++)
                if (c.menu().getUpgradeCount(type, tier) > 0) height += rowHeight(c, type, tier);
        return height;
    }

    private static int rowAt(MythicMinerScreenContext c, double x, double y) {
        int viewportHeight = Math.max(1, c.playerInventoryY() + 70 - VIEWPORT_Y - 8);
        if (!inside(x, y, X + 8, VIEWPORT_Y, c.imageWidth() - 72, viewportHeight)) return -1;
        int rowY = VIEWPORT_Y - c.attributeScroll();
        for (MythicMinerUpgradeBlock.Type type : TYPES) {
            for (int tier = 1; tier <= 6; tier++) {
                if (c.menu().getUpgradeCount(type, tier) <= 0) continue;
                if (inside(x, y, X + 8, rowY, c.imageWidth() - 72, 20)) return key(type, tier);
                rowY += rowHeight(c, type, tier);
            }
        }
        return -1;
    }

    private static int rowHeight(
            MythicMinerScreenContext c, MythicMinerUpgradeBlock.Type type, int tier) {
        if (!c.upgradeRowExpanded(key(type, tier))) return 22;
        int lines =
                switch (type) {
                    case ENERGY -> 2;
                    case AGGREGATE -> 5;
                    default -> 1;
                };
        return 26 + lines * 12;
    }

    private static int key(MythicMinerUpgradeBlock.Type type, int tier) {
        return type.ordinal() * 10 + tier;
    }

    private static String upgradeTranslationKey(MythicMinerUpgradeBlock.Type type, int tier) {
        String key =
                "block.dimension_tech.mythic_miner_upgrade_" + type.name().toLowerCase(Locale.ROOT);
        return tier == 1 ? key : key + "_tier_" + tier;
    }

    private static List<Component> tooltip(
            String name,
            Object value,
            int bonus,
            boolean reduction,
            int specialized,
            int aggregate) {
        return List.of(
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute." + name + "_value", value),
                Component.translatable(
                        reduction
                                ? "screen.dimension_tech.mythic_miner.attribute.reduction"
                                : "screen.dimension_tech.mythic_miner.attribute.bonus",
                        percent(bonus)),
                Component.translatable(
                        "screen.dimension_tech.mythic_miner.attribute.upgrade_breakdown",
                        specialized,
                        aggregate));
    }

    private static boolean inside(
            double mouseX, double mouseY, int x, int y, int width, int height) {
        return MythicMinerScreen.inside(mouseX, mouseY, x, y, width, height);
    }

    private static String decimal(int value) {
        return String.format(Locale.ROOT, "%.2f", value / 100.0D);
    }

    private static String percent(int value) {
        return BigDecimal.valueOf(value, 2).stripTrailingZeros().toPlainString();
    }

    private static String configPercent(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String bonus(int value, boolean reduction) {
        return value == 0 ? "(0%)" : "(" + (reduction ? "-" : "+") + percent(value) + "%)";
    }

    private static String compact(int value) {
        if (value >= 1_000_000_000)
            return String.format(Locale.ROOT, "%.2fB", value / 1_000_000_000.0D);
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.2fM", value / 1_000_000.0D);
        if (value >= 1_000) return String.format(Locale.ROOT, "%.2fK", value / 1_000.0D);
        return Integer.toString(value);
    }

    static boolean isAttributesPage(MythicMinerScreen.Page page) {
        return page == MythicMinerScreen.Page.ATTRIBUTES;
    }
}
