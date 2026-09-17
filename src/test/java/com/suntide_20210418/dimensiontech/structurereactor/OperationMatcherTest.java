package com.suntide_20210418.dimensiontech.structurereactor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;

/**
 * The display view of an operation. Production recipes carry an ingredient; predicate-backed
 * recipes, used by the cycle's own tests and by scripting, carry none.
 */
class OperationMatcherTest {
    @Test
    void predicateOperationsExposeNoIngredient() {
        OperationMatcher<String> matcher = OperationMatcher.of(value -> value.equals("branch"));

        assertFalse(matcher.asIngredient().isPresent());
        assertFalse(matcher.isEmpty());
        assertTrue(matcher.test("branch"));
        assertFalse(matcher.test("recurse"));
    }

    @Test
    void ingredientOperationsExposeTheirIngredient() {
        // Ingredient.EMPTY is a plain constant, so this stays free of item registry access.
        Ingredient ingredient = Ingredient.EMPTY;

        OperationMatcher<ItemStack> matcher = OperationMatcher.of(ingredient);

        assertTrue(matcher.asIngredient().isPresent());
        assertSame(ingredient, matcher.asIngredient().orElseThrow());
        assertTrue(matcher.isEmpty());
    }
}
