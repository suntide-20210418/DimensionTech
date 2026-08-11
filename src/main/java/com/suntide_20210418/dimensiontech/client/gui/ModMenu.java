package com.suntide_20210418.dimensiontech.client.gui;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;

public class ModMenu {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, DimensionTechMod.MOD_ID);

}
