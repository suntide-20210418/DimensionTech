package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, DimensionTechMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Blocks with hand-authored models under src/main/resources reference them via
        // getExistingFile; generating a placeholder for these would either collide with the
        // hand-written file (fails processResources) or shadow it.
        //
        // Both the reactor and the operator front the +Z side of their hand-authored model.
        // horizontalBlock assumes the model fronts the north (-Z) face and would render our
        // +Z-fronted models turned halfway round, so they use horizontalFrontingSouth instead
        // (see below). Either way the block's front always points at the player when placed.
        // The trailing rotational offset tunes the authored geometry onto the front face: the
        // reactor's front reads a quarter turn counter-clockwise, the operator's a half turn
        // round, from the +Z reference.
        ModelFile reactor =
                models().getExistingFile(modLoc("block/strcture_reactor"));
        horizontalFrontingSouth(ModBlocks.STRUCTURE_REACTOR.get(), reactor, 90);
        simpleBlockItem(ModBlocks.STRUCTURE_REACTOR.get(), reactor);
        ModelFile operator =
                models().getExistingFile(modLoc("block/structure_data_operator"));
        horizontalFrontingSouth(ModBlocks.STRUCTURE_DATA_OPERATOR.get(), operator, 180);
        simpleBlockItem(ModBlocks.STRUCTURE_DATA_OPERATOR.get(), operator);
        simpleBlockWithItem(
                ModBlocks.STRUCTURE_MINER_CASING.get(),
                models().getExistingFile(modLoc("block/structure_miner_casing")));

        registerHandWrittenMiner(ModBlocks.TIER_1_STRUCTURE_MINER.get(), 1);
        registerHandWrittenMiner(ModBlocks.TIER_2_STRUCTURE_MINER.get(), 2);
        registerHandWrittenMiner(ModBlocks.TIER_3_STRUCTURE_MINER.get(), 3);
        registerHandWrittenMiner(ModBlocks.TIER_4_STRUCTURE_MINER.get(), 4);
        registerHandWrittenMiner(ModBlocks.TIER_5_STRUCTURE_MINER.get(), 5);
        registerHandWrittenMiner(ModBlocks.TIER_6_STRUCTURE_MINER.get(), 6);

        simpleBlockWithItem(
                ModBlocks.STRUCTURE_MINER_STRUCTURE.get(),
                models().getExistingFile(modLoc("block/structure_miner_strcture")));
        simpleBlockWithItem(
                ModBlocks.STRUCTURE_MINER_GLASS.get(),
                models().getExistingFile(modLoc("block/structre_miner_glass")));

        registerUpgradeBase(ModBlocks.UPGRADE_PARALLEL);
        registerUpgradeBase(ModBlocks.UPGRADE_LUCK);
        registerUpgradeBase(ModBlocks.UPGRADE_ENERGY);
        registerUpgradeBase(ModBlocks.UPGRADE_EFFICIENCY);
        registerUpgradeBase(ModBlocks.UPGRADE_AGGREGATE);
        registerUpgradeTiers(ModBlocks.UPGRADE_PARALLEL_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_LUCK_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_ENERGY_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_EFFICIENCY_TIERS);
        registerUpgradeTiers(ModBlocks.UPGRADE_AGGREGATE_TIERS);
    }

    /**
     * The six miner tiers ship hand-authored Blockbench models, so datagen only writes the
     * blockstate and the item model, both pointing at
     * {@code dimension_tech:block/tier_N_strcture_miner}. The "strcture" spelling is the
     * on-disk file name from the original Blockbench export and must not be "fixed" here —
     * the block ids are spelled correctly ("structure"), so the two are easy to conflate.
     */
    private void registerHandWrittenMiner(Block block, int tier) {
        ModelFile model = models().getExistingFile(modLoc("block/tier_" + tier + "_strcture_miner"));
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }

    /**
     * Emits the four {@link BlockStateProperties#HORIZONTAL_FACING} variants for a hand-authored
     * model whose front face sits on the +Z (south) side.
     *
     * <p>Upstream {@link #horizontalBlock(Block, ModelFile)} assumes the model fronts the north
     * (-Z) face, so its rotations would leave a +Z-fronted model pointing 180 degrees the wrong
     * way. The rotations below turn the +Z front onto each facing direction directly, then rotate
     * the whole model by {@code rotationOffset} to tune the authored geometry onto the front face
     * (a positive offset is counter-clockwise from above). With the state set to the direction the
     * front should point at, the player-facing arrangement follows.
     */
    private void horizontalFrontingSouth(Block block, ModelFile model, int rotationOffset) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        facingVariant(builder, Direction.NORTH, model, rot(180 + rotationOffset));
        facingVariant(builder, Direction.EAST, model, rot(270 + rotationOffset));
        facingVariant(builder, Direction.SOUTH, model, rot(rotationOffset));
        facingVariant(builder, Direction.WEST, model, rot(90 + rotationOffset));
    }

    private static int rot(int value) {
        return ((value % 360) + 360) % 360;
    }

    private void facingVariant(
            VariantBlockStateBuilder builder, Direction facing, ModelFile model, int rotationY) {
        builder.partialState()
                .with(BlockStateProperties.HORIZONTAL_FACING, facing)
                .modelForState()
                .modelFile(model)
                .rotationY(rotationY)
                .addModel();
    }

    /** The base upgrade block is tier 1 and shares the hand-written tier-1 model. */
    private void registerUpgradeBase(RegistryObject<Block> base) {
        ModelFile model =
                models().getExistingFile(modLoc("block/" + base.getId().getPath() + "_tier_1"));
        simpleBlockWithItem(base.get(), model);
    }

    private void registerUpgradeTiers(RegistryObject<Block>[] tiers) {
        for (int tier = 1; tier < tiers.length; tier++) {
            Block block = tiers[tier].get();
            ModelFile model =
                    models().getExistingFile(modLoc("block/" + tiers[tier].getId().getPath()));
            simpleBlockWithItem(block, model);
        }
    }
}
