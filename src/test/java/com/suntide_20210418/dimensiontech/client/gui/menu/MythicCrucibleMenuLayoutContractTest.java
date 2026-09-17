package com.suntide_20210418.dimensiontech.client.gui.menu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MythicCrucibleMenuLayoutContractTest {
    private static final Path MENU =
            Path.of("src/main/java/com/suntide_20210418/dimensiontech/client/gui/menu/MythicCrucibleMenu.java");

    @Test
    void menuSlotsUseTheSharedTextureLayout() throws Exception {
        String source = Files.readString(MENU);
        assertTrue(source.contains("MythicCrucibleLayout.FRAGMENT_SLOT"));
        assertTrue(source.contains("MythicCrucibleLayout.OPERATION_SLOT"));
        assertTrue(source.contains("MythicCrucibleLayout.playerSlotX(column)"));
        assertTrue(source.contains("MythicCrucibleLayout.playerSlotY(row)"));
        assertTrue(source.contains("MythicCrucibleLayout.hotbarSlotX(column)"));
        assertTrue(source.contains("MythicCrucibleLayout.hotbarSlotY()"));
    }
}
