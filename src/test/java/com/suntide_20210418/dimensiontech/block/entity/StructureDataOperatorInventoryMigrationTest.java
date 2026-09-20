package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/**
 * Guards the operator's inventory-tag migration.
 *
 * <p>The write array grew from 27 to 36 slots, which moved the two plugin slots from 28/29 to 37/38.
 * {@code ItemStackHandler.deserializeNBT} takes its capacity from the {@code Size} field it is handed
 * rather than from the constructor — that is what the crash proved: the reported bound was
 * {@code [0,30)}, and 30 is exactly {@code 1 + 27 + 2}, the old capacity. A pre-change world therefore
 * loads a 30-slot handler while the menu registers 39, and the first {@code broadcastChanges} reads
 * slot 30 out of range and kills the server thread.
 *
 * <p>These tests use plain {@link CompoundTag}s because an {@code ItemStackHandler} cannot be built
 * without a bootstrapped Minecraft: constructing one in a bare JUnit run dies in a static
 * initialiser. The behaviour being compensated for is already evidenced by the crash itself.
 */
class StructureDataOperatorInventoryMigrationTest {
    /**
     * What a save written with the old 27-slot write array carries: the read slot, the write array,
     * and the two plugin slots.
     */
    private static final int LEGACY_SIZE = 1 + 27 + 2;

    /** The two indices the plugin slots used to live at, and still live at in a legacy tag. */
    private static final int LEGACY_INTEGRATOR = 28;

    private static final int LEGACY_INTERPRETER = 29;

    @Test
    void theLayoutThisMigratesFromIsStillTheOneTested() {
        assertEquals(30, LEGACY_SIZE, "30 is exactly what the crash reported as the handler's size");
        assertEquals(37, StructureDataOperatorBlockEntity.INTEGRATOR);
        assertEquals(38, StructureDataOperatorBlockEntity.INTERPRETER);
        assertEquals(39, StructureDataOperatorBlockEntity.INVENTORY_SIZE);
    }

    @Test
    void aLegacyTagIsWidenedToTheCurrentCapacity() {
        CompoundTag migrated = migrate(LEGACY_SIZE, 0, LEGACY_INTEGRATOR, LEGACY_INTERPRETER);

        assertEquals(StructureDataOperatorBlockEntity.INVENTORY_SIZE, migrated.getInt("Size"));
    }

    @Test
    void thePluginSlotsAreMovedOffWhatAreNowWriteSlots() {
        CompoundTag migrated = migrate(LEGACY_SIZE, 0, LEGACY_INTEGRATOR, LEGACY_INTERPRETER);

        assertTrue(holdsSlot(migrated, 0), "the read slot keeps its index");
        assertTrue(
                holdsSlot(migrated, StructureDataOperatorBlockEntity.INTEGRATOR),
                "the Data Integrator moves to the slot the menu reads for it");
        assertTrue(
                holdsSlot(migrated, StructureDataOperatorBlockEntity.INTERPRETER),
                "the Structure Interpreter moves too");
        assertFalse(
                holdsSlot(migrated, LEGACY_INTEGRATOR),
                "28 is a write slot now, so nothing may be left behind in it");
        assertFalse(holdsSlot(migrated, LEGACY_INTERPRETER), "29 is a write slot now as well");
    }

    @Test
    void writeSlotsKeepTheirIndicesAcrossTheWidening() {
        CompoundTag migrated = migrate(LEGACY_SIZE, 1, 27, 0);

        assertTrue(holdsSlot(migrated, 1));
        assertTrue(holdsSlot(migrated, 27), "the old array only grew at its end");
        assertEquals(3, itemCount(migrated), "no entry is dropped on the way through");
    }

    @Test
    void aCurrentTagIsLeftAlone() {
        CompoundTag migrated =
                migrate(StructureDataOperatorBlockEntity.INVENTORY_SIZE, 0, 5, 37, 38);

        assertEquals(StructureDataOperatorBlockEntity.INVENTORY_SIZE, migrated.getInt("Size"));
        assertEquals(4, itemCount(migrated));
        assertTrue(holdsSlot(migrated, 37), "a tag that needs no migration keeps its plugin slots");
        assertTrue(holdsSlot(migrated, 38));
    }

    @Test
    void aTagFromAWiderBuildIsClampedRatherThanTrusted() {
        CompoundTag migrated = migrate(64, 0, 60);

        assertEquals(
                StructureDataOperatorBlockEntity.INVENTORY_SIZE,
                migrated.getInt("Size"),
                "capacity comes from the code, never from the save file");
        /*
         * The stray entry at 60 stays in the tag: ItemStackHandler.deserializeNBT drops whatever
         * sits past the capacity it was handed, and serializeNBT only writes back slots that exist.
         * Filtering it here too would just duplicate the parent. What matters is that shrinking
         * invents no plugin slot.
         */
        assertFalse(
                holdsSlot(migrated, StructureDataOperatorBlockEntity.INTEGRATOR),
                "a shrink must not fabricate a plugin slot");
    }

    @Test
    void aTagTooSmallToHoldTheOldLayoutIsOnlyClamped() {
        CompoundTag migrated = migrate(2, 0, 1);

        assertEquals(StructureDataOperatorBlockEntity.INVENTORY_SIZE, migrated.getInt("Size"));
        assertTrue(
                holdsSlot(migrated, 0),
                "a tag this small cannot be an old layout, so the read slot must stay put");
        assertTrue(holdsSlot(migrated, 1), "and so must the slot beside it");
    }

    private static CompoundTag migrate(int size, int... occupiedSlots) {
        return StructureDataOperatorBlockEntity.migrateInventory(tag(size, occupiedSlots));
    }

    private static CompoundTag tag(int size, int... occupiedSlots) {
        ListTag items = new ListTag();
        for (int slot : occupiedSlots) {
            CompoundTag entry = new CompoundTag();
            /* The item itself is irrelevant here; only the slot index is rewritten. */
            entry.putInt("Slot", slot);
            items.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        tag.putInt("Size", size);
        return tag;
    }

    private static boolean holdsSlot(CompoundTag inventoryTag, int slot) {
        ListTag items = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            if (items.getCompound(index).getInt("Slot") == slot) return true;
        }
        return false;
    }

    private static int itemCount(CompoundTag inventoryTag) {
        return inventoryTag.getList("Items", Tag.TAG_COMPOUND).size();
    }
}
