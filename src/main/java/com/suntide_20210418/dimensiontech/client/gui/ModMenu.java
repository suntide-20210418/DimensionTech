package com.suntide_20210418.dimensiontech.client.gui;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureDataOperatorMenu;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureReactorMenu;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenu {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, DimensionTechMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StructureMinerMenu>> STRUCTURE_MINER =
            MENU_TYPES.register(
                    ResourceLocationHelper.getPath(
                            ResourceLocationHelper.modLoc("structure_miner")),
                    () -> IForgeMenuType.create(StructureMinerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<StructureReactorMenu>>
            STRUCTURE_REACTOR =
                    MENU_TYPES.register(
                            "structure_reactor",
                            () -> IForgeMenuType.create(StructureReactorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<StructureDataOperatorMenu>>
            STRUCTURE_DATA_OPERATOR =
                    MENU_TYPES.register(
                            "structure_data_operator",
                            () -> IForgeMenuType.create(StructureDataOperatorMenu::new));

    private ModMenu() {}
}
