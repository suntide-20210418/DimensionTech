package com.suntide_20210418.dimensiontech.mythicminer.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EquipmentDismantlerTest {
    @Test
    void diamondChestplateReturnsEightDiamonds() {
        assertEquals(
                8,
                EquipmentDismantler.materialCount(
                        EquipmentDismantler.EquipmentPattern.CHESTPLATE));
    }

    @Test
    void diamondSwordReturnsTwoDiamonds() {
        assertEquals(
                2,
                EquipmentDismantler.materialCount(EquipmentDismantler.EquipmentPattern.SWORD));
    }

    @Test
    void ironPickaxeReturnsThreeIronIngots() {
        assertEquals(
                3,
                EquipmentDismantler.materialCount(EquipmentDismantler.EquipmentPattern.PICKAXE));
    }

    @Test
    void equipmentExpectationsAreMergedIntoMaterialExpectations() {
        ResourceLocation diamond =
                ResourceLocation.fromNamespaceAndPath("minecraft", "diamond");
        Map<ResourceLocation, Double> result = new LinkedHashMap<>();

        EquipmentDismantler.mergeExpectation(result, diamond, 0.5D, 8);
        EquipmentDismantler.mergeExpectation(result, diamond, 2.0D, 2);

        assertEquals(8.0D, result.get(diamond), 0.000_001D);
    }
}
