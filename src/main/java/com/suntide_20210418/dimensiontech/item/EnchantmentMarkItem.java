package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.utils.TranslateHelper;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** A stackable mark whose NBT identifies one enchantment type. */
public final class EnchantmentMarkItem extends Item {
    private static final String ENCHANTMENT_TAG = "Enchantment";

    public EnchantmentMarkItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(ResourceLocation enchantmentId, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(ModItems.ENCHANTMENT_MARK.get(), count);
        result.getOrCreateTag().putString(ENCHANTMENT_TAG, enchantmentId.toString());
        return result;
    }

    public static Optional<ResourceLocation> getEnchantmentId(ItemStack stack) {
        if (!stack.is(ModItems.ENCHANTMENT_MARK.get())) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(tag.getString(ENCHANTMENT_TAG)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        Optional<ResourceLocation> enchantmentId = getEnchantmentId(stack);
        if (enchantmentId.isEmpty()) {
            tooltip.add(
                    TranslateHelper.translate(TranslateHelper.tooltip("enchantment_mark.unbound"))
                            .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId.get());
        Component enchantmentName =
                enchantment == null
                        ? Component.literal(enchantmentId.get().toString())
                        : Component.translatable(enchantment.getDescriptionId());
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("enchantment_mark.enchantment"),
                                enchantmentName.copy().withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
    }
}
