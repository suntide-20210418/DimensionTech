package com.suntide_20210418.dimensiontech.client.key;

import com.suntide_20210418.dimensiontech.client.StructMarkerClient;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DimensionTechMod.MOD_ID, value = Dist.CLIENT)
public class KeyEvent {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ModKey.MODE_SWITCH_KEY.consumeClick()) {
            StructMarkerClient.markHeldMarker();
        }
    }
}
