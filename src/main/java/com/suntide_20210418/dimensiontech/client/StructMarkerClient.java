package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.client.gui.screen.StructMarkerScreen;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The client-side entry points the network layer calls into for the structure marker.
 *
 * <p>Both of them are reached through {@code DistExecutor.unsafeRunWhenOn(CLIENT, ...)}, so this
 * class is only ever class-loaded on a physical client and may reference client-only types freely.
 */
public final class StructMarkerClient {
    private StructMarkerClient() {}

    /**
     * Shows the marker terminal, or re-feeds the one that is already open.
     *
     * <p>Marking a structure round-trips through the server and comes back as a freshly built
     * stack. When that answer arrives while the terminal is already up for the same hand, the open
     * screen is handed the new stack rather than replaced: rebuilding it would throw away the
     * scroll position and flash the whole background on every press of an action button.
     */
    public static void open(ItemStack marker, InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof StructMarkerScreen open && open.holds(hand)) {
            open.refresh(marker);
            return;
        }
        minecraft.setScreen(new StructMarkerScreen(marker, hand));
    }

    /**
     * Hands the overlapping-structure choices to the terminal that asked for them.
     *
     * <p>Silently dropped when no terminal is open: the server pushes choices in answer to a mark
     * request, and the player may have closed the screen in between.
     */
    public static void showStructureChoices(
            InteractionHand hand, List<StructMarkerItem.MarkedStructure> structures) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof StructMarkerScreen markerScreen) {
            markerScreen.showStructureChoices(hand, structures);
        }
    }
}
