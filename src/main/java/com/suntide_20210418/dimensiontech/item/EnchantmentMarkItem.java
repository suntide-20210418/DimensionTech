package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.loot.expectation.EnchantmentKey;
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

/**
 * A stackable mark carrying one enchantment type at one concrete level.
 *
 * <p>The economy is binary and level-preserving: one mark of level L splits into two marks of level
 * L - 1, and two marks of level L combine into one mark of level L + 1. Level 1 is therefore the
 * atomic unit and a level L enchantment is worth 2^(L-1) of them. Marks of different enchantments
 * never combine.
 */
public final class EnchantmentMarkItem extends Item {
    private static final String ENCHANTMENT_TAG = "Enchantment";
    private static final String LEVEL_TAG = "Level";

    /** Hard cap kept independently of any enchantment so a corrupt tag cannot allocate wildly. */
    public static final int MAX_LEVEL = 10;

    public EnchantmentMarkItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(ResourceLocation enchantmentId, int level, int count) {
        if (count <= 0 || level <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(ModItems.ENCHANTMENT_MARK.get(), count);
        CompoundTag tag = result.getOrCreateTag();
        tag.putString(ENCHANTMENT_TAG, enchantmentId.toString());
        tag.putInt(LEVEL_TAG, level);
        return result;
    }

    public static ItemStack create(EnchantmentKey key, int count) {
        return create(key.enchantment(), key.level(), count);
    }

    public static Optional<EnchantmentKey> getKey(ItemStack stack) {
        Optional<ResourceLocation> id = getEnchantmentId(stack);
        if (id.isEmpty()) return Optional.empty();
        int level = getLevel(stack);
        if (level <= 0) return Optional.empty();
        return Optional.of(new EnchantmentKey(id.get(), level));
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

    public static int getLevel(ItemStack stack) {
        if (!stack.is(ModItems.ENCHANTMENT_MARK.get())) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(LEVEL_TAG);
    }

    /** True when two marks describe the same enchantment at the same level. */
    public static boolean sameKey(ItemStack left, ItemStack right) {
        return getKey(left).equals(getKey(right)) && getKey(left).isPresent();
    }

    /** Highest level a mark of this enchantment may reach, clamped to {@link #MAX_LEVEL}. */
    public static int maxLevelFor(ResourceLocation enchantmentId) {
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        int max = enchantment == null ? MAX_LEVEL : enchantment.getMaxLevel();
        return Math.max(1, Math.min(MAX_LEVEL, max));
    }

    public static int maxLevelFor(EnchantmentKey key) {
        return maxLevelFor(key.enchantment());
    }

    /** Result of splitting one mark into two of the next lower level, or empty when atomic. */
    public static Optional<ItemStack> splitResult(ItemStack stack) {
        Optional<EnchantmentKey> key = getKey(stack);
        if (key.isEmpty() || key.get().level() <= 1) return Optional.empty();
        return Optional.of(create(key.get().enchantment(), key.get().level() - 1, 2));
    }

    /** Result of combining two marks into one of the next higher level, or empty when capped. */
    public static Optional<ItemStack> combineResult(ItemStack stack) {
        Optional<EnchantmentKey> key = getKey(stack);
        if (key.isEmpty()) return Optional.empty();
        int level = key.get().level();
        if (level >= maxLevelFor(key.get())) return Optional.empty();
        return Optional.of(create(key.get().enchantment(), level + 1, 1));
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

        int markLevel = getLevel(stack);
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId.get());
        Component name =
                enchantment == null
                        ? Component.literal(enchantmentId.get().toString())
                        : enchantment.getFullname(Math.max(1, markLevel));
        tooltip.add(
                TranslateHelper.translate(
                                TranslateHelper.tooltip("enchantment_mark.enchantment"),
                                name.copy().withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(
                TranslateHelper.translate(TranslateHelper.tooltip("enchantment_mark.level"), markLevel)
                        .withStyle(ChatFormatting.DARK_GRAY));
        if (enchantment != null && markLevel > 1) {
            tooltip.add(
                    TranslateHelper.translate(
                                    TranslateHelper.tooltip("enchantment_mark.split_hint"),
                                    (1L << (markLevel - 1)))
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
