package com.suntide_20210418.dimensiontech.block;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import java.math.BigDecimal;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MythicMinerUpgradeBlock extends Block {
    public enum Type {
        NONE,
        PARALLEL,
        LUCK,
        ENERGY,
        EFFICIENCY,
        AGGREGATE
    }

    private final Type type;
    private final int tier;

    public MythicMinerUpgradeBlock(BlockBehaviour.Properties properties, Type type, int tier) {
        super(properties);
        this.type = type;
        if (tier < 1 || tier > 6) throw new IllegalArgumentException("Upgrade tier must be 1-6");
        this.tier = tier;
    }

    public Type getType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable BlockGetter level,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ModConfigs.MythicMinerUpgradeTierConfig config = upgradeConfig();
        switch (type) {
            case EFFICIENCY -> addEfficiencyTooltip(tooltip, config);
            case ENERGY -> {
                addCapacityTooltip(tooltip, config);
                addConsumptionTooltip(tooltip, config, false);
            }
            case PARALLEL -> addParallelTooltip(tooltip, config);
            case LUCK -> addLuckTooltip(tooltip, config);
            case AGGREGATE -> {
                addEfficiencyTooltip(tooltip, config);
                addCapacityTooltip(tooltip, config);
                addConsumptionTooltip(tooltip, config, true);
                addParallelTooltip(tooltip, config);
                addLuckTooltip(tooltip, config);
            }
            case NONE ->
                    tooltip.add(
                            Component.translatable(
                                            "tooltip.dimension_tech.mythic_miner.upgrade.none")
                                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private ModConfigs.MythicMinerUpgradeTierConfig upgradeConfig() {
        return (type == Type.AGGREGATE
                        ? ModConfigs.AGGREGATE_UPGRADE_TIERS
                        : ModConfigs.UPGRADE_TIERS)
                [tier - 1];
    }

    private static void addEfficiencyTooltip(
            List<Component> tooltip, ModConfigs.MythicMinerUpgradeTierConfig config) {
        addPercentTooltip(
                tooltip,
                "tooltip.dimension_tech.mythic_miner.upgrade.efficiency",
                config.efficiencyIncreasePercent());
    }

    private static void addCapacityTooltip(
            List<Component> tooltip, ModConfigs.MythicMinerUpgradeTierConfig config) {
        addPercentTooltip(
                tooltip,
                "tooltip.dimension_tech.mythic_miner.upgrade.energy_capacity",
                config.energyCapacityIncreasePercent());
    }

    private static void addConsumptionTooltip(
            List<Component> tooltip,
            ModConfigs.MythicMinerUpgradeTierConfig config,
            boolean additive) {
        addPercentTooltip(
                tooltip,
                additive
                        ? "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.additive"
                        : "tooltip.dimension_tech.mythic_miner.upgrade.energy_consumption.multiplicative",
                config.energyConsumptionReductionPercent());
    }

    private static void addParallelTooltip(
            List<Component> tooltip, ModConfigs.MythicMinerUpgradeTierConfig config) {
        addPercentTooltip(
                tooltip,
                "tooltip.dimension_tech.mythic_miner.upgrade.parallel",
                config.parallelIncreasePercent());
    }

    private static void addLuckTooltip(
            List<Component> tooltip, ModConfigs.MythicMinerUpgradeTierConfig config) {
        addPercentTooltip(
                tooltip,
                "tooltip.dimension_tech.mythic_miner.upgrade.luck",
                config.luckIncreasePercent());
    }

    private static void addPercentTooltip(
            List<Component> tooltip, String translationKey, double value) {
        String formattedValue = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        tooltip.add(
                Component.translatable(translationKey, formattedValue)
                        .withStyle(ChatFormatting.AQUA));
    }
}
