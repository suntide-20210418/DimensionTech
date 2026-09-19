package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StructureReactorRecipeChangesTest {
    @Test
    void statusDisplayUsesPreviousStatusContract() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/suntide_20210418/dimensiontech/client/gui/screen/StructureReactorScreen.java"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("KEY_STATUS_PREVIOUS"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("previousStatusText"));
    }
}
