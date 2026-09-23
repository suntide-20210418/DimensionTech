package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StructureReactorMenuLayoutContractTest {
    private static final Path MENU =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/client/gui/menu/StructureReactorMenu.java");

    @Test
    void menuSlotsUseTheSharedTextureLayout() throws Exception {
        String source = Files.readString(MENU);
        assertTrue(source.contains("StructureReactorLayout.FRAGMENT_SLOT"));
        assertTrue(source.contains("StructureReactorLayout.OPERATION_SLOT"));
        assertTrue(source.contains("StructureReactorLayout.playerSlotX(column)"));
        assertTrue(source.contains("StructureReactorLayout.playerSlotY(row)"));
        assertTrue(source.contains("StructureReactorLayout.hotbarSlotX(column)"));
        assertTrue(source.contains("StructureReactorLayout.hotbarSlotY()"));
    }
}
