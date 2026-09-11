package com.suntide_20210418.dimensiontech.mythiccrucible;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The one operation an item can supply to a state. The cycle only needs to ask whether a supplied
 * stack belongs to an operation, which keeps the state machine independent of registries and
 * levels: production recipes carry an {@link Ingredient} while tests can carry a plain predicate.
 *
 * @param <S> the supplied stack type, an {@link ItemStack} in production
 */
public interface OperationMatcher<S> {
    boolean test(S stack);

    default boolean isEmpty() {
        return false;
    }

    /** Adapts a vanilla ingredient so recipes keep their existing datapack-facing shape. */
    static OperationMatcher<ItemStack> of(Ingredient ingredient) {
        return new IngredientOperation(ingredient);
    }

    /** Adapts a plain predicate, used by unit tests. */
    static <S> OperationMatcher<S> of(java.util.function.Predicate<S> predicate) {
        return predicate::test;
    }

    /** A vanilla ingredient exposed as an operation. */
    record IngredientOperation(Ingredient ingredient) implements OperationMatcher<ItemStack> {
        @Override
        public boolean test(ItemStack stack) {
            return ingredient.test(stack);
        }

        @Override
        public boolean isEmpty() {
            return ingredient.isEmpty();
        }
    }
}
