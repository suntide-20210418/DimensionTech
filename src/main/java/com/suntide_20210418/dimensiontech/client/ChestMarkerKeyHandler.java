package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for the chest marker's analyse key.
 *
 * <p>Pressing the key (default V) with a chest marker in hand sends the block under the crosshair to
 * the server, which validates it as a loot-table container and marks it. The client uses its own
 * crosshair hit rather than re-raycasting on the server, so the block the player actually sees is
 * the block that gets marked.
 */
@Mod.EventBusSubscriber(modid = DimensionTechMod.MOD_ID, value = Dist.CLIENT)
public final class ChestMarkerKeyHandler {
    private ChestMarkerKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!KeyBindings.CHEST_ANALYSE.consumeClick()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) return;
        InteractionHand hand = holdingChestMarker(player);
        if (hand == null) return;
        if (minecraft.hitResult instanceof BlockHitResult blockHit) {
            BlockPos position = blockHit.getBlockPos();
            ModNetwork.requestChestAnalysis(hand, position);
        }
    }

    private static InteractionHand holdingChestMarker(LocalPlayer player) {
        if (player.getMainHandItem().is(ModItems.CHEST_MARKER.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(ModItems.CHEST_MARKER.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
