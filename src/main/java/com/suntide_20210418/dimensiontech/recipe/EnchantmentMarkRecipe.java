package com.suntide_20210418.dimensiontech.recipe;

import com.suntide_20210418.dimensiontech.item.EnchantmentMarkItem;
import com.suntide_20210418.dimensiontech.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Splits or combines enchantment marks in the vanilla crafting grid.
 *
 * <p>{@code split}: one mark of level L (L >= 2) becomes two marks of level L - 1.
 * {@code combine}: two marks of the same enchantment and level L become one mark of level L + 1,
 * capped at that enchantment's maximum level. Marks of different enchantments never interact, which
 * falls out of comparing the whole {@code EnchantmentKey} rather than the enchantment id alone.
 *
 * <p>Both directions are deliberately value-preserving: a level L mark is worth 2^(L-1) level 1
 * marks in either direction.
 */
public final class EnchantmentMarkRecipe implements CraftingRecipe {
    private final ResourceLocation id;
    private final Mode mode;

    public EnchantmentMarkRecipe(ResourceLocation id, Mode mode) {
        this.id = id;
        this.mode = mode;
    }

    public enum Mode {
        SPLIT,
        COMBINE
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return output(container).isPresent();
    }

    /** Resolves the output for the marks present in a grid, ignoring empty slots. */
    public static Optional<ItemStack> resolve(List<ItemStack> marks, Mode mode) {
        if (marks.isEmpty()) return Optional.empty();
        for (ItemStack mark : marks) {
            if (!mark.is(ModItems.ENCHANTMENT_MARK.get())) return Optional.empty();
        }
        if (mode == Mode.SPLIT) {
            if (marks.size() != 1) return Optional.empty();
            return EnchantmentMarkItem.splitResult(marks.get(0));
        }
        if (marks.size() != 2) return Optional.empty();
        if (!EnchantmentMarkItem.sameKey(marks.get(0), marks.get(1))) return Optional.empty();
        return EnchantmentMarkItem.combineResult(marks.get(0));
    }

    private Optional<ItemStack> output(CraftingContainer container) {
        List<ItemStack> marks = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!stack.is(ModItems.ENCHANTMENT_MARK.get())) return Optional.empty();
            marks.add(stack);
        }
        return resolve(marks, mode);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        return output(container).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        // NBT-dependent, so there is no single result item to show in the recipe book.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(9, Ingredient.EMPTY);
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ENCHANTMENT_MARK.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public static final class Serializer implements RecipeSerializer<EnchantmentMarkRecipe> {
        @Override
        public EnchantmentMarkRecipe fromJson(ResourceLocation recipeId, com.google.gson.JsonObject json) {
            String mode = GsonHelper.getAsString(json, "mode", "split");
            Mode parsed;
            try {
                parsed = Mode.valueOf(mode.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new com.google.gson.JsonSyntaxException(
                        "Unknown enchantment mark recipe mode: " + mode);
            }
            return new EnchantmentMarkRecipe(recipeId, parsed);
        }

        @Nullable
        @Override
        public EnchantmentMarkRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new EnchantmentMarkRecipe(recipeId, buffer.readEnum(Mode.class));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, EnchantmentMarkRecipe recipe) {
            buffer.writeEnum(recipe.mode);
        }
    }
}
