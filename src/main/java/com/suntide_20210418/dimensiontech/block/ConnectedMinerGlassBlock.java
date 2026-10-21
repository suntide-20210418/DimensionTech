package com.suntide_20210418.dimensiontech.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Clear cutout glass: adjacent glass never submits its hidden shared face. */
public final class ConnectedMinerGlassBlock extends AssemblyPanelBlock {
    public ConnectedMinerGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}
