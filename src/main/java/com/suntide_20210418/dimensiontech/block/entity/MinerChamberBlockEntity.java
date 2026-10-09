package com.suntide_20210418.dimensiontech.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base of the three miner chambers.
 *
 * <p>A chamber is owned by exactly one miner and has no ticker of its own: the miner binds itself
 * on every tick and then calls {@link #chamberTick}. The binding is what lets the chamber answer
 * capability questions (fluid validity, for instance) that arrive outside the miner's own tick,
 * without the chamber having to search the 5x5x5 body for whichever controller owns it.
 */
public abstract class MinerChamberBlockEntity extends BlockEntity {
    private BaseMinerBlockEntity miner;

    protected MinerChamberBlockEntity(
            BlockEntityType<?> type, BlockPos position, BlockState blockState) {
        super(type, position, blockState);
    }

    /** Records the miner that owns this chamber; called by the miner on every tick. */
    public final void bindMiner(BaseMinerBlockEntity miner) {
        this.miner = miner;
    }

    /**
     * The owning miner, or {@code null} when none has bound this chamber yet or the previous owner
     * has since been removed — a broken controller must not keep answering for the chamber.
     */
    @Nullable
    public final BaseMinerBlockEntity miner() {
        if (miner == null || miner.isRemoved()) return null;
        return miner;
    }

    /** Server-side tick, driven by the owning miner. */
    public abstract void chamberTick(BaseMinerBlockEntity miner);
}
