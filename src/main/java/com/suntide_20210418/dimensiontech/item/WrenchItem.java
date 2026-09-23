package com.suntide_20210418.dimensiontech.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * The projection wrench. Plain right-click on a structure miner toggles the multiblock projection;
 * shift+right-click builds the multiblock out of the player's inventory. The tooltip spells out
 * both actions.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    /**
     * Returning {@code true} keeps the structure miner's {@code Block.use} reachable while
     * sneaking. By default a plain item's sneak+right-click skips the block and uses the item
     * instead, so the shift+build branch would never run.
     */
    @Override
    public boolean doesSneakBypassUse(
            ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(
                Component.translatable("tooltip.dimension_tech.wrench.projection")
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable("tooltip.dimension_tech.wrench.build")
                        .withStyle(ChatFormatting.GRAY));
    }
}
