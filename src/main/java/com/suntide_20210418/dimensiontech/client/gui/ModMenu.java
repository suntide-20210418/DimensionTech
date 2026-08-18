package com.suntide_20210418.dimensiontech.client.gui;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenu {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, DimensionTechMod.MOD_ID);

    public static final RegistryObject<MenuType<MythicMinerMenu>> MYTHIC_MINER =
            MENU_TYPES.register(
                    ResourceLocationHelper.getPath(ResourceLocationHelper.modLoc("mythic_miner")),
                    () -> IForgeMenuType.create(MythicMinerMenu::new));

    private ModMenu() {}
}
