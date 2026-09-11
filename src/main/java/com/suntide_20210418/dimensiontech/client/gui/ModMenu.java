package com.suntide_20210418.dimensiontech.client.gui;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicCrucibleMenu;
import com.suntide_20210418.dimensiontech.client.gui.menu.MythicMinerMenu;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
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
    public static final RegistryObject<MenuType<StructureDataOperatorMenu>>
            STRUCTURE_DATA_OPERATOR =
                    MENU_TYPES.register(
                            "structure_data_operator",
                            () -> IForgeMenuType.create(StructureDataOperatorMenu::new));
    public static final RegistryObject<MenuType<MythicCrucibleMenu>> MYTHIC_CRUCIBLE =
            MENU_TYPES.register(
                    "mythic_crucible", () -> IForgeMenuType.create(MythicCrucibleMenu::new));

    private ModMenu() {}
}
