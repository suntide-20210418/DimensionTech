package com.suntide_20210418.dimensiontech.client.model;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.StructureMinerUpgradeBlock;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = DimensionTechMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ConnectedPanelModels {
    private ConnectedPanelModels() {}

    @SubscribeEvent
    public static void registerMaterials(ModelEvent.RegisterAdditional event) {
        for (String name : PanelMaterialBank.names()) event.register(PanelMaterialBank.id(name));
    }

    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        PanelMaterialBank bank = new PanelMaterialBank(event.getModels());
        Map<Key, BakedModel> wrappers = new HashMap<>();
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            Block block = holder.get();
            ConnectedPanelModel.Kind kind;
            String type = "";
            int tier = 0;
            if (block == ModBlocks.STRUCTURE_MINER_CASING.get())
                kind = ConnectedPanelModel.Kind.CASING;
            else if (block == ModBlocks.STRUCTURE_MINER_GLASS.get())
                kind = ConnectedPanelModel.Kind.GLASS;
            else if (block == ModBlocks.STRUCTURE_MINER_STRUCTURE.get())
                kind = ConnectedPanelModel.Kind.STRUCTURE;
            else if (block instanceof StructureMinerUpgradeBlock upgrade) {
                kind = ConnectedPanelModel.Kind.MODULE;
                type = upgrade.getType().name().toLowerCase(Locale.ROOT);
                tier = upgrade.getTier();
            } else continue;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var id = BlockModelShaper.stateToModelLocation(state);
                BakedModel original = event.getModels().get(id);
                if (original == null) throw new IllegalStateException("Missing panel model: " + id);
                Key key = new Key(original, kind, type, tier);
                event.getModels()
                        .put(
                                id,
                                wrappers.computeIfAbsent(
                                        key,
                                        k ->
                                                new ConnectedPanelModel(
                                                        k.original(),
                                                        k.kind(),
                                                        k.type(),
                                                        k.tier(),
                                                        bank)));
            }
        }
    }

    private record Key(BakedModel original, ConnectedPanelModel.Kind kind, String type, int tier) {}
}
