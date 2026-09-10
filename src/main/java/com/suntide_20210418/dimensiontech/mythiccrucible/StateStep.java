package com.suntide_20210418.dimensiontech.mythiccrucible;

import java.util.Objects;
import net.minecraft.world.item.crafting.Ingredient;

/** A state and the one operation item that settles it. */
public record StateStep(StateId state, Ingredient operation) {
    public StateStep {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(operation, "operation");
        if (operation.isEmpty()) throw new IllegalArgumentException("operation must not be empty");
    }
}
