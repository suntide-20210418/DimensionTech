package com.suntide_20210418.dimensiontech.integration.jade;

import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.FluidInputChamberBlock;
import com.suntide_20210418.dimensiontech.block.ItemOutputChamberBlock;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.block.entity.FluidInputChamberBlockEntity;
import com.suntide_20210418.dimensiontech.block.entity.ItemOutputChamberBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("dimension_tech")
public final class StructureMinerJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                StructureMinerJadeProvider.INSTANCE, BaseMinerBlockEntity.class);
        // The chambers carry their own readouts rather than riding on the miner's: what the player
        // hovers is the block whose state they want, and only one of the two is bound to a machine.
        registration.registerBlockDataProvider(
                FluidInputChamberJadeProvider.INSTANCE, FluidInputChamberBlockEntity.class);
        registration.registerBlockDataProvider(
                ItemOutputChamberJadeProvider.INSTANCE, ItemOutputChamberBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                StructureMinerJadeProvider.INSTANCE, BaseMinerBlock.class);
        registration.registerBlockComponent(
                FluidInputChamberJadeProvider.INSTANCE, FluidInputChamberBlock.class);
        registration.registerBlockComponent(
                ItemOutputChamberJadeProvider.INSTANCE, ItemOutputChamberBlock.class);
    }
}
