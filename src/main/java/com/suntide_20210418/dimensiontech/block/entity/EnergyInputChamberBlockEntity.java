package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.energy.EnergyContainer;
import com.suntide_20210418.dimensiontech.energy.SimpleEnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The miner's FE buffer, carried by the energy input chamber.
 *
 * <p><b>The capacity is mirrored, not invented.</b> The chamber takes its capacity from the miner
 * it is bound to. That value only exists in a meaningful form once the multiblock stands, because
 * the controller's upgrade bonuses are cleared while it does not — so the last value read from a
 * complete machine is cached, and an incomplete machine falls back to that cache, or to {@link
 * #DEFAULT_CAPACITY_FE} when nothing has ever been cached. That is what keeps a chamber placed
 * ahead of the build able to accept energy at all.
 */
public final class EnergyInputChamberBlockEntity extends MinerChamberBlockEntity {
    /** Capacity used before the chamber has ever read a complete machine. */
    public static final int DEFAULT_CAPACITY_FE = 10_000;

    private static final String ENERGY_TAG = "Energy";
    private static final String CACHED_CAPACITY_TAG = "CachedCapacity";

    private final SimpleEnergyContainer energy =
            new SimpleEnergyContainer(DEFAULT_CAPACITY_FE, true, false, ignored -> setChanged());

    /** Last capacity read from a complete miner; 0 means nothing has been cached yet. */
    private int cachedCapacity;

    public EnergyInputChamberBlockEntity(BlockPos position, BlockState blockState) {
        super(ModBlockEntities.ENERGY_INPUT_CHAMBER.get(), position, blockState);
    }

    public EnergyContainer energy() {
        return energy;
    }

    public int cachedCapacity() {
        return cachedCapacity;
    }

    @Override
    public void chamberTick(BaseMinerBlockEntity miner) {
        int capacity;
        if (miner.isStructureComplete()) {
            capacity = miner.getEffectiveEnergyCapacity();
            if (capacity != cachedCapacity) {
                cachedCapacity = capacity;
                setChanged();
            }
        } else {
            capacity = cachedCapacity > 0 ? cachedCapacity : DEFAULT_CAPACITY_FE;
        }
        if (energy.getMaxEnergyStored() != capacity) {
            energy.setCapacity(capacity);
            setChanged();
        }
    }

    /** Right-click reports the stored energy and the machine's per-thread consumption. */
    public void handleUse(Player player) {
        BaseMinerBlockEntity owner = miner();
        // Two keys rather than a Component argument: a translated Component dropped into a %s slot
        // renders as the argument's own description id, not its text.
        Component message =
                owner == null
                        ? Component.translatable(
                                "message.dimension_tech.energy_input_chamber.contents_unbound",
                                energy.getEnergyStored(),
                                energy.getMaxEnergyStored())
                        : Component.translatable(
                                "message.dimension_tech.energy_input_chamber.contents",
                                energy.getEnergyStored(),
                                energy.getMaxEnergyStored(),
                                owner.getEffectiveThreadEnergyConsumption());
        player.displayClientMessage(message, false);
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        energy.save(tag, ENERGY_TAG);
        tag.putInt(CACHED_CAPACITY_TAG, cachedCapacity);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // The cache is restored before the stored energy so a value above the 10,000 default is not
        // clamped away on the way in.
        cachedCapacity = Math.max(0, tag.getInt(CACHED_CAPACITY_TAG));
        if (cachedCapacity > 0) energy.setCapacity(cachedCapacity);
        energy.load(tag, ENERGY_TAG);
    }
}
