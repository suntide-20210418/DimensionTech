package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.block.entity.StructureDataOperatorBlockEntity;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureReactorMenu;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureDataOperatorScreen;
import com.suntide_20210418.dimensiontech.client.gui.screen.StructureMinerScreen;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.network.payload.OperatorCataloguePacket;
import com.suntide_20210418.dimensiontech.structurereactor.ReactorTooltipSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端专用的 payload 处理体。
 *
 * <p>这里集中了唯一会触碰客户端类（{@code Minecraft}、界面类）的逻辑。各 payload 的 {@code handle}
 * 必须先用 {@code FMLEnvironment.dist.isClient()} 守卫再调用本类，否则专用服务端会去解析本类引用的
 * 客户端类而类加载失败。
 */
public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {}

    public static void openRefreshedMarker(ItemStack marker, InteractionHand hand) {
        if (marker.is(ModItems.CHEST_MARKER.get())) {
            ChestMarkerClient.open(marker, hand);
        } else {
            StructMarkerClient.open(marker, hand);
        }
    }

    public static void showStructureChoices(
            InteractionHand hand,
            BlockPos position,
            List<StructMarkerItem.MarkedStructure> structures) {
        StructMarkerClient.showStructureChoices(hand, position, structures);
    }

    public static void receiveMinerAnalysis(
            int containerId,
            int slot,
            double dimensionValue,
            double structureValue,
            boolean equipmentDismantling,
            Map<ResourceLocation, Double> itemExpectations,
            Set<ResourceLocation> disabledItems) {
        StructureMinerScreen.receiveAnalysis(
                containerId,
                slot,
                dimensionValue,
                structureValue,
                equipmentDismantling,
                itemExpectations,
                disabledItems);
    }

    public static void receiveReactorTooltip(int containerId, ReactorTooltipSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu instanceof StructureReactorMenu menu
                && menu.containerId == containerId) {
            menu.applyTooltipSnapshot(snapshot);
        }
    }

    public static void receiveOperatorCatalogue(List<OperatorCataloguePacket.CatalogueRow> rows) {
        if (Minecraft.getInstance().screen instanceof StructureDataOperatorScreen screen) {
            List<StructureDataOperatorBlockEntity.StructureCatalogueEntry> entries =
                    new ArrayList<>(rows.size());
            for (OperatorCataloguePacket.CatalogueRow row : rows) {
                entries.add(
                        new StructureDataOperatorBlockEntity.StructureCatalogueEntry(
                                row.dimension(), row.structure()));
            }
            screen.receiveCatalogue(entries);
        }
    }

    public static void receiveOperatorAnalysis(
            ResourceLocation dimension, ResourceLocation structure, ItemStack marker) {
        if (Minecraft.getInstance().screen instanceof StructureDataOperatorScreen screen) {
            screen.receiveDetail(dimension, structure, marker);
        }
    }
}