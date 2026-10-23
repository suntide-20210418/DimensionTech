package com.suntide_20210418.dimensiontech.integration.jade;

import com.suntide_20210418.dimensiontech.block.BaseMinerBlock;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
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
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                StructureMinerJadeProvider.INSTANCE, BaseMinerBlock.class);
    }
}
