package com.suntide_20210418.dimensiontech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Static chunk-rendered panel. Slot zero is a loose block; 1..66 identify its position in the
 * assembled rig. Vanilla chunk packets persist/sync this value.
 */
public class AssemblyPanelBlock extends Block {
    public static final IntegerProperty ASSEMBLY_SLOT =
            IntegerProperty.create("assembly_slot", 0, 66);

    public AssemblyPanelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ASSEMBLY_SLOT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ASSEMBLY_SLOT);
    }

    @Override
    public void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!state.is(old.getBlock())) StructureMinerVisuals.markNearbyControllers(level, pos);
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()))
            StructureMinerVisuals.markNearbyControllers(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
