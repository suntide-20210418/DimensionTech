package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.client.gui.screen.StructMarkerScreen;
import com.suntide_20210418.dimensiontech.network.ModNetwork;
import com.suntide_20210418.dimensiontech.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class StructMarkerClient {
    private StructMarkerClient() {}

    public static void open(ItemStack marker) {
        Minecraft.getInstance().setScreen(new StructMarkerScreen(marker));
    }

    public static void markHeldMarker() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) return;
        InteractionHand hand = markerHand(minecraft);
        if (hand != null) ModNetwork.mark(hand);
    }

    private static InteractionHand markerHand(Minecraft minecraft) {
        if (minecraft.player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.STRUCT_MARKER.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (minecraft.player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.STRUCT_MARKER.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
