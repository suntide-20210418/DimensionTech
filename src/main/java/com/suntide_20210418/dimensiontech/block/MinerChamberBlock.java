package com.suntide_20210418.dimensiontech.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base of the three casing-shaped chambers that a structure miner can take instead of a plain
 * casing block.
 *
 * <p><b>What "replaceable casing" means here.</b> Each chamber is a full cube with a hand-authored
 * model, exactly like {@code structure_miner_casing}, so it can stand in any of the pattern's 39
 * casing cells. {@link StructureMinerMultiblock#acceptsCasingSlot} is the single place that decides
 * that, so the planner, the completeness test and the client overlay cannot disagree. The chamber
 * then carries one of the machine's I/O roles; the miner locates them by walking the casing cells
 * in a fixed order and takes the first of each kind, so a second chamber of the same kind only acts
 * as an ordinary casing.
 *
 * <p><b>Why the miner ticks them.</b> The chambers have no block ticker of their own. The miner is
 * the only object that can reach them without a reverse lookup (a chamber cannot tell which
 * controller owns it without scanning every candidate in the 5x5x5 body), and it already walks the
 * pattern every tick for its own settlement. Driving them from there keeps one ticker and one
 * lookup instead of two.
 */
public abstract class MinerChamberBlock extends BaseEntityBlock {
    protected MinerChamberBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }
}
