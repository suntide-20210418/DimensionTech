package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.structureminer.output.EquipmentDismantler;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime-registry checks for equipment dismantling.
 *
 * <p>The client-side preview passes a {@code null} level because it has no recipe manager. Only the
 * primary-material path may run there; everything that would need a recipe lookup has to fall back
 * to leaving the item intact instead of dereferencing the missing level.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EquipmentDismantleGameTests {
    private EquipmentDismantleGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void nullLevelKeepsItemsThatWouldNeedARecipeLookup(GameTestHelper helper) {
        // These have no primary material, so they are the ones that reach the recipe lookup.
        for (ItemStack equipment :
                List.of(
                        new ItemStack(Items.SHIELD),
                        new ItemStack(Items.BOW),
                        new ItemStack(Items.CROSSBOW))) {
            List<ItemStack> result;
            try {
                result = EquipmentDismantler.dismantle(null, equipment);
            } catch (RuntimeException exception) {
                helper.fail(
                        "Dismantling "
                                + equipment.getItem().getClass().getSimpleName()
                                + " with a null level threw "
                                + exception);
                return;
            }
            if (result.size() != 1 || !result.get(0).is(equipment.getItem())) {
                helper.fail(
                        "A null level must leave "
                                + equipment.getItem()
                                + " intact, got "
                                + result);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void nullLevelStillResolvesPrimaryMaterials(GameTestHelper helper) {
        List<ItemStack> chestplate = EquipmentDismantler.dismantle(null, new ItemStack(Items.DIAMOND_CHESTPLATE));
        if (chestplate.size() != 1
                || !chestplate.get(0).is(Items.DIAMOND)
                || chestplate.get(0).getCount() != 8) {
            helper.fail("Diamond chestplate did not dismantle into eight diamonds: " + chestplate);
            return;
        }
        List<ItemStack> sword = EquipmentDismantler.dismantle(null, new ItemStack(Items.IRON_SWORD));
        if (sword.size() != 1 || !sword.get(0).is(Items.IRON_INGOT) || sword.get(0).getCount() != 2) {
            helper.fail("Iron sword did not dismantle into two iron ingots: " + sword);
            return;
        }
        // Elytra are a primary-material case too: the special path yields phantom membranes.
        List<ItemStack> elytra = EquipmentDismantler.dismantle(null, new ItemStack(Items.ELYTRA));
        if (elytra.size() != 1
                || !elytra.get(0).is(Items.PHANTOM_MEMBRANE)
                || elytra.get(0).getCount() != 6) {
            helper.fail("Elytra did not dismantle into six phantom membranes: " + elytra);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void nonEquipmentIsReturnedUnchanged(GameTestHelper helper) {
        ItemStack dirt = new ItemStack(Items.DIRT, 3);
        List<ItemStack> result = EquipmentDismantler.dismantle(null, dirt);
        if (result.size() != 1 || !result.get(0).is(Items.DIRT) || result.get(0).getCount() != 3) {
            helper.fail("A non-equipment stack changed during dismantling: " + result);
            return;
        }
        helper.succeed();
    }
}
