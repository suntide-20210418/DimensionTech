package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Pins the per-thread disabled-item contract.
 *
 * <p>Before this, one machine-wide set meant disabling a drop on thread 1 also disabled it on every
 * other thread. These tests keep the scope honest and keep the pre-change save format readable.
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MinerOutputControllerGameTests {
    private MinerOutputControllerGameTests() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 40)
    public static void disabledItemsAreScopedPerSlot(GameTestHelper helper) {
        MinerOutputController controller = new MinerOutputController();
        ResourceLocation diamond = id("diamond");
        controller.toggleDisabledItem(0, diamond);
        if (!controller.disabled(0, diamond)) {
            helper.fail("Slot 0 should report the item as disabled");
            return;
        }
        if (controller.disabled(1, diamond)) {
            helper.fail("Slot 1 must not inherit an item disabled on slot 0");
            return;
        }
        controller.toggleDisabledItem(1, diamond);
        controller.toggleDisabledItem(0, diamond);
        if (controller.disabled(0, diamond)) {
            helper.fail("Toggling slot 0 twice must re-enable the item");
            return;
        }
        if (!controller.disabled(1, diamond)) {
            helper.fail("Untouched slot 1 must keep its own disabled item");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 40)
    public static void replaceIsIdempotent(GameTestHelper helper) {
        MinerOutputController controller = new MinerOutputController();
        Set<ResourceLocation> ids = Set.of(id("diamond"), id("emerald"));
        if (!controller.replaceDisabledItems(1, ids)) {
            helper.fail("Replacing an empty set with a non-empty one must report a change");
            return;
        }
        if (controller.replaceDisabledItems(1, ids)) {
            helper.fail("Replacing with an identical set must report no change");
            return;
        }
        if (!controller.replaceDisabledItems(1, Set.of())) {
            helper.fail("Clearing a non-empty set must report a change");
            return;
        }
        if (controller.disabled(1, id("diamond"))) {
            helper.fail("A cleared slot must enable everything again");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 40)
    public static void perSlotSetsSurviveSaveAndLoad(GameTestHelper helper) {
        MinerOutputController controller = new MinerOutputController();
        controller.toggleDisabledItem(0, id("diamond"));
        controller.toggleDisabledItem(2, id("emerald"));
        CompoundTag tag = new CompoundTag();
        controller.save(tag, "Equipment", "Pending");

        MinerOutputController restored = new MinerOutputController();
        restored.load(tag, "Equipment", "Pending", 4);
        if (!restored.disabled(0, id("diamond"))) {
            helper.fail("Slot 0 lost its disabled item across a save/load round trip");
            return;
        }
        if (!restored.disabled(2, id("emerald"))) {
            helper.fail("Slot 2 lost its disabled item across a save/load round trip");
            return;
        }
        if (restored.disabled(1, id("diamond"))) {
            helper.fail("Slot 1 gained an item it never disabled");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 40)
    public static void flatLegacySaveSeedsEverySlot(GameTestHelper helper) {
        // The pre-per-thread payload was one flat list of item ids shared by the whole machine.
        CompoundTag legacy = new CompoundTag();
        ListTag flat = new ListTag();
        flat.add(StringTag.valueOf("minecraft:diamond"));
        legacy.put("DisabledExpectedItems", flat);

        MinerOutputController controller = new MinerOutputController();
        controller.load(legacy, "Equipment", "Pending", 3);
        for (int slot = 0; slot < 3; slot++) {
            if (!controller.disabled(slot, id("diamond"))) {
                helper.fail(
                        "A legacy machine-wide save must seed slot "
                                + slot
                                + " rather than silently re-enabling what the player turned off");
                return;
            }
        }
        helper.succeed();
    }
}
