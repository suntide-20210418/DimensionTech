package com.suntide_20210418.dimensiontech.client.gui.screen;

import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.StructureMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerLayout;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * ATTRIBUTES page (A1): a single full-width column of sections, scrollable as one list.
 *
 * <p>The previous two-column layout (A3) put the attribute readout in an 88px column where Chinese
 * labels truncated, and the composition breakdown landed entirely below the fold. A1 gives the
 * readout the full column and keeps it as the first, always-shown section, so the key attributes
 * are never crowded or hidden behind the upgrade list. Composition and upgrades become two
 * collapsible sections the player opens on demand.
 *
 * <p>One {@link #contentHeight} function is the only source of truth for the page's shape. The draw
 * pass, the hit test and the tooltip all advance through the same terms, so collapsing a section
 * cannot leave a clickable row where nothing is drawn — the failure mode sectioned pages are
 * famous for.
 *
 * <p>This page is read-only. Upgrades are placed and removed through the multiblock itself; adding
 * install/uninstall controls here would create a second source of truth for the machine's shape.
 */
final class StructureMinerAttributesPage {
    private static final int ATTR_ROWS = 5;
    private static final int GROUP_ROW_H = 20;
    private static final int GROUP_CHILD_H = 11;
    private static final int UPGRADE_LINE_H = 12;
    private static final int GROUP_GAP = 3;

    /**
     * Collapse keys for the two sections, distinct from the A3-era group/upgrade key spaces.
     * Sections default to collapsed: the first screen is the readout alone, which is the point.
     */
    private static final int SECTION_COMPOSITION_KEY = -100;
    private static final int SECTION_UPGRADES_KEY = -101;

    /** Display order, independent of the enum's own order, which starts at {@code NONE}. */
    private static final StructureMinerUpgradeBlock.Type[] UPGRADE_ORDER = {
        StructureMinerUpgradeBlock.Type.EFFICIENCY,
        StructureMinerUpgradeBlock.Type.ENERGY,
        StructureMinerUpgradeBlock.Type.PARALLEL,
        StructureMinerUpgradeBlock.Type.LUCK,
        StructureMinerUpgradeBlock.Type.AGGREGATE
    };

    private record Line(String text, int railColor) {}

    private record Group(String label, String total, List<Line> children) {}

    private StructureMinerAttributesPage() {}

    static void render(StructureMinerScreenContext c, GuiGraphics g) {
        drawHeaderChips(c, g);

        int viewportX = StructureMinerInfoLayout.ATTR_LIST_X;
        int viewportY = StructureMinerInfoLayout.ATTR_LIST_Y;
        int viewportH = StructureMinerInfoLayout.ATTR_LIST_H;

        int contentHeight = contentHeight(c);
        c.attributeContentHeight(contentHeight);
        int maxScroll = Math.max(0, contentHeight - viewportH);
        int scroll = Math.max(0, Math.min(c.attributeScroll(), maxScroll));
        c.attributeScroll(scroll);

        StructureMinerLayout.ScissorBounds scissor =
                StructureMinerLayout.scaleToScreen(
                        c.leftPos() + viewportX,
                        c.topPos() + viewportY,
                        StructureMinerInfoLayout.ATTR_LIST_W,
                        viewportH,
                        c.uiScale());
        g.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());

        int y = viewportY - scroll;
        y = plainHeader(c, g, StructureMinerInfoLayout.ATTR_COL_X, y, "info.section.marker");
        y = drawOverview(c, g, StructureMinerInfoLayout.ATTR_COL_X, y);
        y =
                collapsibleHeader(
                        c, g, StructureMinerInfoLayout.ATTR_COL_X, y, SECTION_COMPOSITION_KEY, "attribute.composition", null);
        if (c.upgradeRowExpanded(SECTION_COMPOSITION_KEY)) {
            y = drawComposition(c, g, StructureMinerInfoLayout.ATTR_COL_X, y);
        }
        y =
                collapsibleHeader(
                        c,
                        g,
                        StructureMinerInfoLayout.ATTR_COL_X,
                        y,
                        SECTION_UPGRADES_KEY,
                        "attribute.installed",
                        upgradeCountText(c));
        if (c.upgradeRowExpanded(SECTION_UPGRADES_KEY)) {
            y = drawUpgrades(c, g, StructureMinerInfoLayout.ATTR_COL_X, y);
        }

        g.disableScissor();

        StructureMinerTheme.scrollbar(
                g,
                StructureMinerInfoLayout.SCROLLBAR_X,
                viewportY,
                viewportH,
                contentHeight,
                viewportH,
                scroll);
    }

    // --- header chips -------------------------------------------------------

    private static void drawHeaderChips(StructureMinerScreenContext c, GuiGraphics g) {
        StructureMinerMenu menu = c.menu();
        int x = StructureMinerInfoLayout.CONTENT_X;
        int y = StructureMinerInfoLayout.ATTR_HEADER_Y;
        int width = StructureMinerInfoLayout.WORK_CHIP_W;
        int stride = width + StructureMinerInfoLayout.WORK_CHIP_GAP;

        StructureMinerTheme.statusChip(
                g,
                c.font(),
                x,
                y,
                width,
                Component.literal("Tier " + minerTier(c)),
                StructureMinerTheme.FLUIX);

        boolean complete = menu.telemetrySnapshot().structureComplete();
        StructureMinerTheme.statusChip(
                g,
                c.font(),
                x + stride,
                y,
                width,
                Component.translatable(
                        complete
                                ? "screen.dimension_tech.structure_miner.structure.complete"
                                : "screen.dimension_tech.structure_miner.structure_incomplete"),
                complete ? StructureMinerTheme.SUCCESS : StructureMinerTheme.ERROR);

        if (!menu.supportsEquipmentDismantling()) return;
        boolean dismantling = menu.isEquipmentDismantlingEnabled();
        StructureMinerTheme.statusChip(
                g,
                c.font(),
                x + stride * 2,
                y,
                width,
                Component.translatable(
                        dismantling
                                ? "screen.dimension_tech.structure_miner.equipment_dismantling_on"
                                : "screen.dimension_tech.structure_miner.equipment_dismantling_off"),
                dismantling ? StructureMinerTheme.SUCCESS : StructureMinerTheme.DIM);
    }

    private static int minerTier(StructureMinerScreenContext c) {
        return c.menu().getBlockEntity().getBlockState().getBlock() instanceof BaseMinerBlock miner
                ? miner.minerTier()
                : 1;
    }

    // --- section 1: the readout, always shown -------------------------------

    private static int plainHeader(
            StructureMinerScreenContext c, GuiGraphics g, int x, int y, String shortKey) {
        StructureMinerTheme.sectionHeader(
                g,
                c.font(),
                x,
                y,
                StructureMinerInfoLayout.ATTR_COL_W,
                Component.translatable("screen.dimension_tech.structure_miner." + shortKey),
                StructureMinerTheme.FLUIX);
        return y + StructureMinerInfoLayout.SECTION_HEADER_H;
    }

    private static int drawOverview(StructureMinerScreenContext c, GuiGraphics g, int x, int y) {
        StructureMinerMenu menu = c.menu();
        y =
                attributeRow(
                        c,
                        g,
                        x,
                        y,
                        "attribute.efficiency",
                        StructureMinerScreen.formatDecimal(menu.getEfficiencyHundredths()),
                        StructureMinerTheme.FLUIX);
        y =
                attributeRow(
                        c,
                        g,
                        x,
                        y,
                        "attribute.capacity",
                        StructureMinerScreen.formatCompact(menu.getEnergyCapacity()) + " FE",
                        StructureMinerTheme.AMBER);
        y =
                attributeRow(
                        c,
                        g,
                        x,
                        y,
                        "attribute.consumption",
                        StructureMinerScreen.formatCompact(menu.getEffectiveEnergyConsumption())
                                + " FE/t",
                        StructureMinerTheme.AMBER);
        y =
                attributeRow(
                        c,
                        g,
                        x,
                        y,
                        "attribute.parallel",
                        StructureMinerScreen.formatCompact(menu.getBaseParallel()),
                        StructureMinerTheme.FLUIX);
        y =
                attributeRow(
                        c,
                        g,
                        x,
                        y,
                        "attribute.luck",
                        StructureMinerScreen.formatDecimal(menu.getLuckHundredths()),
                        StructureMinerTheme.FLUIX);
        return y;
    }

    private static int attributeRow(
            StructureMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            String labelKey,
            String value,
            int accent) {
        Font font = c.font();
        g.fill(x, y + 1, x + 2, y + StructureMinerInfoLayout.ROW_H_DATA - 3, accent);
        g.drawString(
                font,
                font.plainSubstrByWidth(
                        Component.translatable("screen.dimension_tech.structure_miner." + labelKey)
                                .getString(),
                        StructureMinerInfoLayout.ATTR_COL_W - 36),
                x + 6,
                y + 2,
                StructureMinerTheme.INK,
                false);
        g.drawString(font, value, x + StructureMinerInfoLayout.ATTR_COL_W - font.width(value), y + 2, accent, false);
        return y + StructureMinerInfoLayout.ROW_H_DATA;
    }

    // --- collapsible sections ------------------------------------------------

    /**
     * A section header the player can open and close. Drawn like the A3 group rows rather than the
     * plain label, because the +/- affordance must read as clickable, not as a heading.
     */
    private static int collapsibleHeader(
            StructureMinerScreenContext c,
            GuiGraphics g,
            int x,
            int y,
            int key,
            String shortKey,
            String total) {
        boolean expanded = c.upgradeRowExpanded(key);
        Font font = c.font();
        g.fill(x, y - 1, x + StructureMinerInfoLayout.ATTR_COL_W, y + GROUP_ROW_H - 3, StructureMinerTheme.STRIPE_INK);
        g.fill(x, y - 1, x + 2, y + GROUP_ROW_H - 3, StructureMinerTheme.FLUIX);
        g.drawString(font, expanded ? "-" : "+", x + 4, y + 5, StructureMinerTheme.INK, false);
        String label =
                Component.translatable("screen.dimension_tech.structure_miner." + shortKey)
                        .getString();
        int totalWidth = total == null ? 0 : font.width(total);
        g.drawString(
                font,
                font.plainSubstrByWidth(label, Math.max(1, StructureMinerInfoLayout.ATTR_COL_W - 20 - totalWidth)),
                x + 12,
                y + 5,
                StructureMinerTheme.INK,
                false);
        if (total != null) {
            g.drawString(
                    font,
                    total,
                    x + StructureMinerInfoLayout.ATTR_COL_W - totalWidth - 3,
                    y + 5,
                    StructureMinerTheme.FLUIX,
                    false);
        }
        return y + GROUP_ROW_H;
    }

    // --- section 2: composition, shown fully once open -----------------------

    private static int drawComposition(StructureMinerScreenContext c, GuiGraphics g, int x, int y) {
        Font font = c.font();
        for (Group group : groups(c)) {
            int totalWidth = font.width(group.total());
            g.fill(x, y - 1, x + StructureMinerInfoLayout.ATTR_COL_W, y + GROUP_ROW_H - 3, StructureMinerTheme.STRIPE_INK);
            g.fill(x, y - 1, x + 2, y + GROUP_ROW_H - 3, StructureMinerTheme.FLUIX);
            g.drawString(
                    font,
                    font.plainSubstrByWidth(group.label(), Math.max(1, StructureMinerInfoLayout.ATTR_COL_W - 18 - totalWidth)),
                    x + 10,
                    y + 5,
                    StructureMinerTheme.INK,
                    false);
            g.drawString(
                    font,
                    group.total(),
                    x + StructureMinerInfoLayout.ATTR_COL_W - totalWidth - 3,
                    y + 5,
                    StructureMinerTheme.FLUIX,
                    false);
            y += GROUP_ROW_H;
            for (Line line : group.children()) {
                g.fill(x + 8, y + 1, x + 10, y + GROUP_CHILD_H - 4, line.railColor());
                g.drawString(
                        font,
                        font.plainSubstrByWidth(line.text(), StructureMinerInfoLayout.ATTR_COL_W - 16),
                        x + 12,
                        y,
                        StructureMinerTheme.INK,
                        false);
                y += GROUP_CHILD_H;
            }
            y += GROUP_GAP;
        }
        return y;
    }

    // --- section 3: installed upgrades, shown fully once open ----------------

    private static int drawUpgrades(StructureMinerScreenContext c, GuiGraphics g, int x, int y) {
        Font font = c.font();
        boolean any = false;
        for (StructureMinerUpgradeBlock.Type type : UPGRADE_ORDER) {
            for (int tier = 1; tier <= 6; tier++) {
                int count = c.menu().getUpgradeCount(type, tier);
                if (count <= 0) continue;
                any = true;
                g.renderItem(new ItemStack(upgradeIcon(type)), x, y + 1);
                String name =
                        Component.translatable(upgradeTranslationKey(type, tier)).getString()
                                + "  x"
                                + count;
                g.drawString(
                        font,
                        font.plainSubstrByWidth(name, StructureMinerInfoLayout.ATTR_COL_W - 24),
                        x + 20,
                        y + 5,
                        StructureMinerTheme.INK,
                        false);
                y += GROUP_ROW_H;
                for (Line line : upgradeBonusLines(type, upgradeConfig(type, tier))) {
                    g.drawString(
                            font,
                            font.plainSubstrByWidth(line.text(), StructureMinerInfoLayout.ATTR_COL_W - 24),
                            x + 20,
                            y,
                            line.railColor(),
                            false);
                    y += UPGRADE_LINE_H;
                }
                y += GROUP_GAP;
            }
        }
        if (!any) {
            g.drawString(
                    font,
                    Component.translatable("screen.dimension_tech.structure_miner.attribute.no_upgrades")
                            .getString(),
                    x,
                    y + 5,
                    StructureMinerTheme.DIM,
                    false);
            y += GROUP_ROW_H;
        }
        return y;
    }

    private static String upgradeCountText(StructureMinerScreenContext c) {
        return Component.translatable(
                        "screen.dimension_tech.structure_miner.attribute.total_short",
                        c.menu().getTotalUpgradeCount())
                .getString();
    }

    // --- data assembly -------------------------------------------------------

    /**
     * Each group states where its number comes from, using the same getters the readout above uses.
     * The rows carry whole sentences from the language file rather than label/value pairs, because
     * the existing keys already read as "有效效率：1.00" and splitting them would mean duplicating them.
     */
    private static List<Group> groups(StructureMinerScreenContext c) {
        StructureMinerMenu menu = c.menu();
        int fluix = StructureMinerTheme.FLUIX;
        int amber = StructureMinerTheme.AMBER;
        int success = StructureMinerTheme.SUCCESS;
        List<Group> groups = new ArrayList<>(5);

        groups.add(
                new Group(
                        text(c, "attribute.efficiency"),
                        StructureMinerScreen.formatDecimal(menu.getEfficiencyHundredths()),
                        List.of(
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.efficiency_value",
                                                        StructureMinerScreen.formatDecimal(100))
                                                .getString(),
                                        fluix),
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.bonus",
                                                        StructureMinerScreen.formatPercent(
                                                                menu.getEfficiencyBonusHundredths()))
                                                .getString(),
                                        fluix))));

        groups.add(
                new Group(
                        text(c, "attribute.parallel"),
                        StructureMinerScreen.formatCompact(menu.getBaseParallel()),
                        List.of(
                                line(c, "marker_info.parallel.base", menu.getBaseParallel(), fluix),
                                line(
                                        c,
                                        "marker_info.parallel.efficiency",
                                        menu.getEfficiencyUpgradeCount(),
                                        amber),
                                line(
                                        c,
                                        "marker_info.parallel.external",
                                        StructureMinerScreen.formatDecimal(
                                                menu.getExternalAccelerationParallelHundredths()),
                                        success))));

        groups.add(
                new Group(
                        text(c, "attribute.capacity"),
                        StructureMinerScreen.formatCompact(menu.getEnergyCapacity()),
                        List.of(
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.capacity_value",
                                                        StructureMinerScreen.formatCompact(
                                                                menu.getEnergyCapacity()))
                                                .getString(),
                                        amber),
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.bonus",
                                                        StructureMinerScreen.formatPercent(
                                                                menu.getCapacityBonusHundredths()))
                                                .getString(),
                                        amber))));

        groups.add(
                new Group(
                        text(c, "attribute.consumption"),
                        StructureMinerScreen.formatCompact(menu.getEffectiveEnergyConsumption()),
                        List.of(
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.consumption_value",
                                                        StructureMinerScreen.formatCompact(
                                                                menu.getEffectiveEnergyConsumption()))
                                                .getString(),
                                        amber),
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.reduction",
                                                        StructureMinerScreen.formatPercent(
                                                                menu.getConsumptionReductionHundredths()))
                                                .getString(),
                                        amber))));

        groups.add(
                new Group(
                        text(c, "attribute.luck"),
                        StructureMinerScreen.formatDecimal(menu.getLuckHundredths()),
                        List.of(
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.luck_value",
                                                        StructureMinerScreen.formatDecimal(
                                                                menu.getLuckHundredths()))
                                                .getString(),
                                        fluix),
                                new Line(
                                        Component.translatable(
                                                        "screen.dimension_tech.structure_miner.attribute.bonus",
                                                        StructureMinerScreen.formatPercent(
                                                                menu.getLuckBonusHundredths()))
                                                .getString(),
                                        fluix))));

        return groups;
    }

    private static Line line(StructureMinerScreenContext c, String key, Object value, int color) {
        return new Line(
                Component.translatable("screen.dimension_tech.structure_miner." + key, value)
                        .getString(),
                color);
    }

    private static String text(StructureMinerScreenContext c, String key) {
        return Component.translatable("screen.dimension_tech.structure_miner." + key).getString();
    }

    /** ENERGY and AGGREGATE carry more than one line by design. */
    private static List<Line> upgradeBonusLines(
            StructureMinerUpgradeBlock.Type type, ModConfigs.StructureMinerUpgradeTierConfig config) {
        List<Line> lines = new ArrayList<>(5);
        int fluix = StructureMinerTheme.FLUIX;
        int amber = StructureMinerTheme.AMBER;

        if (type == StructureMinerUpgradeBlock.Type.EFFICIENCY
                || type == StructureMinerUpgradeBlock.Type.AGGREGATE) {
            lines.add(bonusLine("upgrade.efficiency", config.efficiencyIncreasePercent(), fluix));
        }
        if (type == StructureMinerUpgradeBlock.Type.ENERGY
                || type == StructureMinerUpgradeBlock.Type.AGGREGATE) {
            lines.add(
                    bonusLine(
                            "upgrade.energy_capacity",
                            config.energyCapacityIncreasePercent(),
                            amber));
            // Specialised energy upgrades scale consumption multiplicatively, aggregate ones
            // additively — a real mechanical difference that the label has to keep.
            lines.add(
                    bonusLine(
                            type == StructureMinerUpgradeBlock.Type.AGGREGATE
                                    ? "upgrade.energy_consumption.additive"
                                    : "upgrade.energy_consumption.multiplicative",
                            config.energyConsumptionReductionPercent(),
                            amber));
        }
        if (type == StructureMinerUpgradeBlock.Type.PARALLEL
                || type == StructureMinerUpgradeBlock.Type.AGGREGATE) {
            lines.add(bonusLine("upgrade.parallel", config.parallelIncreasePercent(), fluix));
        }
        if (type == StructureMinerUpgradeBlock.Type.LUCK
                || type == StructureMinerUpgradeBlock.Type.AGGREGATE) {
            lines.add(bonusLine("upgrade.luck", config.luckIncreasePercent(), fluix));
        }
        return lines;
    }

    private static Line bonusLine(String suffix, double value, int color) {
        return new Line(
                Component.translatable(
                                "tooltip.dimension_tech.structure_miner." + suffix,
                                StructureMinerScreen.formatConfigPercent(value))
                        .getString(),
                color);
    }

    private static ModConfigs.StructureMinerUpgradeTierConfig upgradeConfig(
            StructureMinerUpgradeBlock.Type type, int tier) {
        return (type == StructureMinerUpgradeBlock.Type.AGGREGATE
                        ? ModConfigs.AGGREGATE_UPGRADE_TIERS
                        : ModConfigs.UPGRADE_TIERS)
                [tier - 1];
    }

    private static String upgradeTranslationKey(StructureMinerUpgradeBlock.Type type, int tier) {
        String key =
                "block.dimension_tech.structure_miner_upgrade_" + type.name().toLowerCase(Locale.ROOT);
        return tier == 1 ? key : key + "_tier_" + tier;
    }

    private static Item upgradeIcon(StructureMinerUpgradeBlock.Type type) {
        return switch (type) {
            case EFFICIENCY -> Items.REDSTONE;
            case ENERGY -> Items.BEACON;
            case PARALLEL -> Items.ARROW;
            case LUCK -> Items.RABBIT_FOOT;
            case AGGREGATE -> Items.NETHER_STAR;
            default -> Items.STONE;
        };
    }

    // --- geometry: one source of truth --------------------------------------

    /** Height of the composition content, exactly the terms {@link #drawComposition} advances. */
    private static int compositionContentHeight(StructureMinerScreenContext c) {
        int height = 0;
        for (Group group : groups(c)) {
            height += GROUP_ROW_H + group.children().size() * GROUP_CHILD_H + GROUP_GAP;
        }
        return height;
    }

    /** Height of the upgrades content, exactly the terms {@link #drawUpgrades} advances. */
    private static int upgradesContentHeight(StructureMinerScreenContext c) {
        int height = 0;
        boolean any = false;
        for (StructureMinerUpgradeBlock.Type type : UPGRADE_ORDER) {
            for (int tier = 1; tier <= 6; tier++) {
                if (c.menu().getUpgradeCount(type, tier) <= 0) continue;
                any = true;
                height +=
                        GROUP_ROW_H
                                + upgradeBonusLines(type, upgradeConfig(type, tier)).size()
                                        * UPGRADE_LINE_H
                                + GROUP_GAP;
            }
        }
        return any ? height : GROUP_ROW_H;
    }

    /**
     * The single source of truth for the page's shape. The draw pass and {@link #rowKeyAt} advance
     * through the same terms in the same order, so the scroll range and the clickable rows can
     * never disagree about where a section sits.
     */
    static int contentHeight(StructureMinerScreenContext c) {
        int height =
                StructureMinerInfoLayout.SECTION_HEADER_H
                        + ATTR_ROWS * StructureMinerInfoLayout.ROW_H_DATA;
        height += GROUP_ROW_H;
        if (c.upgradeRowExpanded(SECTION_COMPOSITION_KEY)) height += compositionContentHeight(c);
        height += GROUP_ROW_H;
        if (c.upgradeRowExpanded(SECTION_UPGRADES_KEY)) height += upgradesContentHeight(c);
        return height;
    }

    /**
     * Collapse key under a panel-local point, or {@link Integer#MIN_VALUE}.
     *
     * <p>Only the two section headers are clickable. Everything else on this page is a reading, not
     * a control, so it deliberately resolves to MIN_VALUE.
     */
    static int rowKeyAt(StructureMinerScreenContext c, double x, double y) {
        if (!StructureMinerInfoLayout.inside(
                x,
                y,
                StructureMinerInfoLayout.ATTR_LIST_X,
                StructureMinerInfoLayout.ATTR_LIST_Y,
                StructureMinerInfoLayout.ATTR_LIST_W,
                StructureMinerInfoLayout.ATTR_LIST_H)) {
            return Integer.MIN_VALUE;
        }
        if (x < StructureMinerInfoLayout.ATTR_COL_X || x >= StructureMinerInfoLayout.ATTR_COL_X + StructureMinerInfoLayout.ATTR_COL_W) return Integer.MIN_VALUE;
        int rowTop =
                StructureMinerInfoLayout.ATTR_LIST_Y
                        - c.attributeScroll()
                        + StructureMinerInfoLayout.SECTION_HEADER_H
                        + ATTR_ROWS * StructureMinerInfoLayout.ROW_H_DATA;
        if (y >= rowTop && y < rowTop + GROUP_ROW_H) return SECTION_COMPOSITION_KEY;
        rowTop += GROUP_ROW_H;
        if (c.upgradeRowExpanded(SECTION_COMPOSITION_KEY)) rowTop += compositionContentHeight(c);
        if (y >= rowTop && y < rowTop + GROUP_ROW_H) return SECTION_UPGRADES_KEY;
        return Integer.MIN_VALUE;
    }

    // --- hover ----------------------------------------------------------------

    /**
     * Readout order is efficiency, capacity, consumption, parallel, luck; {@link #groups} holds the
     * same five in a different order, so the hover maps instead of assuming they line up.
     */
    private static final int[] READOUT_TO_GROUP = {0, 2, 3, 1, 4};

    /**
     * Hover pass for the readout rows. A readout row states a value, so its tooltip states where
     * that value comes from — the breakdown its composition group holds.
     */
    static void renderTooltip(
            StructureMinerScreenContext c,
            GuiGraphics g,
            double x,
            double y,
            int screenX,
            int screenY) {
        if (!StructureMinerInfoLayout.inside(
                x,
                y,
                StructureMinerInfoLayout.ATTR_LIST_X,
                StructureMinerInfoLayout.ATTR_LIST_Y,
                StructureMinerInfoLayout.ATTR_LIST_W,
                StructureMinerInfoLayout.ATTR_LIST_H)) {
            return;
        }
        if (x < StructureMinerInfoLayout.ATTR_COL_X || x >= StructureMinerInfoLayout.ATTR_COL_X + StructureMinerInfoLayout.ATTR_COL_W) return;

        int row = overviewRowAt(c, y);
        if (row < 0) return;
        Group group = groups(c).get(READOUT_TO_GROUP[row]);
        List<Component> lines = new ArrayList<>(group.children().size() + 1);
        lines.add(Component.literal(group.label() + "  " + group.total()));
        for (Line line : group.children()) {
            lines.add(Component.literal(line.text()));
        }
        g.renderTooltip(c.font(), lines, Optional.empty(), screenX, screenY);
    }

    /** Index of the readout row under {@code y}, or {@code -1}. Readout rows are not clickable. */
    private static int overviewRowAt(StructureMinerScreenContext c, double y) {
        int top =
                StructureMinerInfoLayout.ATTR_LIST_Y
                        - c.attributeScroll()
                        + StructureMinerInfoLayout.SECTION_HEADER_H;
        if (y < top) return -1;
        int index = (int) Math.floor((y - top) / (double) StructureMinerInfoLayout.ROW_H_DATA);
        return index < ATTR_ROWS ? index : -1;
    }
}
