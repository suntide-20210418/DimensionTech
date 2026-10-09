package com.suntide_20210418.dimensiontech.integration.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Tooltip helpers shared by the Jade providers.
 *
 * <p>Kept in one place so every panel speaks the same key namespace and an empty tank is named the
 * same way everywhere; three providers each carrying their own copy of {@code line(...)} is exactly
 * the kind of small duplication that later drifts.
 */
final class JadeText {
    private JadeText() {}

    /** One {@code jade.dimension_tech.*} row with a single substituted value. */
    static Component line(String key, Component value) {
        return Component.translatable("jade.dimension_tech." + key, value);
    }

    /** The fluid's own display name, or a neutral placeholder for an empty tank. */
    static Component fluidName(Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY
                ? Component.translatable("jade.dimension_tech.fluid_empty")
                : Component.translatable(fluid.getFluidType().getDescriptionId());
    }
}
