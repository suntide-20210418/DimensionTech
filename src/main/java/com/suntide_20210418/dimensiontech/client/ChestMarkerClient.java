package com.suntide_20210418.dimensiontech.client;

import com.suntide_20210418.dimensiontech.client.gui.screen.ChestMarkerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The client-side entry point the network layer calls into for the chest marker.
 *
 * <p>Reached through {@code DistExecutor.unsafeRunWhenOn(CLIENT, ...)} like its structure
 * counterpart, so this class is only ever class-loaded on a physical client.
 */
public final class ChestMarkerClient {
    private ChestMarkerClient() {}

    /**
     * Shows the chest marker terminal, or re-feeds the one that is already open.
     *
     * <p>Marking a chest round-trips through the server and comes back as a freshly built stack.
     * When that answer arrives while the terminal is already up for the same hand, the open screen
     * is handed the new stack rather than replaced, so the scroll position survives.
     */
    public static void open(ItemStack marker, InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ChestMarkerScreen open && open.holds(hand)) {
            open.refresh(marker);
            return;
        }
        minecraft.setScreen(new ChestMarkerScreen(marker, hand));
    }
}
