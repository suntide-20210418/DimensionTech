package com.suntide_20210418.dimensiontech.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SimpleEnergyContainerTest {
    @Test
    void receivesWithSimulationAndClampsCapacity() {
        SimpleEnergyContainer storage = new SimpleEnergyContainer(100);

        assertEquals(100, storage.receiveEnergy(150, true));
        assertEquals(0, storage.getEnergyStored());
        assertEquals(100, storage.receiveEnergy(150, false));
        storage.setCapacity(40);
        assertEquals(40, storage.getEnergyStored());
        assertEquals(40, storage.getMaxEnergyStored());
    }

    @Test
    void internalConsumptionIsAtomicAndNotForgeExtraction() {
        SimpleEnergyContainer storage = new SimpleEnergyContainer(100);
        storage.receiveEnergy(60, false);

        assertFalse(storage.canExtract());
        assertEquals(0, storage.extractEnergy(20, false));
        assertTrue(storage.canConsume(20));
        assertTrue(storage.consume(20));
        assertEquals(40, storage.getEnergyStored());
        assertFalse(storage.consume(50));
        assertEquals(40, storage.getEnergyStored());
    }

    @Test
    void callbackOnlyRunsForActualTransferOrConsumption() {
        AtomicInteger changes = new AtomicInteger();
        SimpleEnergyContainer storage =
                new SimpleEnergyContainer(100, true, false, ignored -> changes.incrementAndGet());

        storage.receiveEnergy(10, true);
        storage.consume(1);
        storage.receiveEnergy(10, false);
        storage.consume(4);

        assertEquals(2, changes.get());
    }

    @Test
    void savesAndRestoresEnergyUsingTheProvidedKey() {
        SimpleEnergyContainer source = new SimpleEnergyContainer(100);
        source.receiveEnergy(73, false);
        CompoundTag tag = new CompoundTag();
        source.save(tag, "Energy");

        SimpleEnergyContainer restored = new SimpleEnergyContainer(50);
        restored.load(tag, "Energy");
        assertEquals(50, restored.getEnergyStored());
    }

    @Test
    void rejectsNonPositiveInitialCapacity() {
        assertThrows(
                IllegalArgumentException.class, () -> new SimpleEnergyContainer(0));
    }
}
