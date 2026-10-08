package com.suntide_20210418.dimensiontech.network.payload;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.entity.StructureMinerAnalysisSnapshot;
import com.suntide_20210418.dimensiontech.client.gui.menu.StructureMinerMenu;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 结构矿机界面一次性启用或停用该标记器的全部预期物品产出。客户端 → 服务端。
//
// 载荷是一个布尔而不是物品 id 集合：集合由服务端从槽内标记器自己的期望表算出，客户端只是表达
// “全选”或“全不选”这个意图。这让界面无法用伪造的 id 列表污染输出配置，也让包体不随掉落物数量
// 增长——一个有两百种产物的结构不会为此多发两百个 id。
//
// 语义是“把停用集合设成这样”而不是“翻转每一项”。翻转只对单个物品是良定义的；批量下发时，一次
// 重传就会把刚设好的状态翻回去，而幂等的赋值不会。
public record StructureMinerExpectedItemBulkPacket(int containerId, int slot, boolean allDisabled)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureMinerExpectedItemBulkPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            DimensionTechMod.MOD_ID, "miner_expected_item_bulk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMinerExpectedItemBulkPacket>
            STREAM_CODEC =
                    StreamCodec.of(
                            StructureMinerExpectedItemBulkPacket::encode,
                            StructureMinerExpectedItemBulkPacket::decode);

    @Override
    public CustomPacketPayload.Type<StructureMinerExpectedItemBulkPacket> type() {
        return TYPE;
    }

    public static void encode(
            RegistryFriendlyByteBuf buffer, StructureMinerExpectedItemBulkPacket payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeVarInt(payload.slot());
        buffer.writeBoolean(payload.allDisabled());
    }

    public static StructureMinerExpectedItemBulkPacket decode(RegistryFriendlyByteBuf buffer) {
        return new StructureMinerExpectedItemBulkPacket(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(
            StructureMinerExpectedItemBulkPacket payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof StructureMinerMenu menu)) return;
        if (menu.containerId != payload.containerId()) return;
        if (payload.slot() < 0 || payload.slot() >= menu.getContainerSlotCount()) return;
        if (!menu.stillValid(player)) return;

        ItemStack marker = menu.slots.get(payload.slot()).getItem();
        Set<ResourceLocation> disabled =
                payload.allDisabled()
                        ? StructMarkerItem.getExpectedItemCounts(marker).keySet()
                        : Set.of();

        // 停用集合没变就说明这次点击是空操作——连点两次同一个按钮，或在全部已启用时又按了全选。
        // 为它重跑一次分析，等于白烧一整轮推导去回传一份与屏幕上完全相同的快照。
        if (!menu.getBlockEntity().setDisabledExpectedItems(disabled)) return;

        menu.getBlockEntity().refreshMarkerAnalysis();
        StructureMinerAnalysisSnapshot snapshot =
                menu.getBlockEntity().getMarkerAnalysisSnapshot(payload.slot());
        PacketDistributor.sendToPlayer(
                player,
                new StructureMinerAnalysisPacket(
                        payload.containerId(),
                        payload.slot(),
                        snapshot.dimensionValue(),
                        snapshot.structureValue(),
                        snapshot.equipmentDismantling(),
                        snapshot.itemExpectations(),
                        snapshot.disabledItems()));
    }
}
