package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.client.gui.screen.StructMarkerScreen;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class StructMarkerClient {
    private StructMarkerClient() {}

    public static void open(ItemStack marker, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new StructMarkerScreen(marker, hand));
    }

    public static void showStructureChoices(
            InteractionHand hand,
            BlockPos position,
            List<StructMarkerItem.MarkedStructure> structures) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof StructMarkerScreen markerScreen) {
            markerScreen.showStructureChoices(hand, position, structures);
        }
    }
}
