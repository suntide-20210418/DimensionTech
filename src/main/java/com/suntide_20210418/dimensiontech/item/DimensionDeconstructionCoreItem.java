package com.suntide_20210418.dimensiontech.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The dimension-deconstruction core. Its only natural source is the structure miner (it drops from
 * the mine with a per-tier chance), so the tooltip states how it is earned and what it crafts.
 */
public class DimensionDeconstructionCoreItem extends Item {
    public DimensionDeconstructionCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.dimension_deconstruction_core.acquire")
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.dimension_deconstruction_core.first")
                        .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(
                Component.translatable(
                                "tooltip.dimension_tech.dimension_deconstruction_core.use")
                        .withStyle(ChatFormatting.GRAY));
    }
}