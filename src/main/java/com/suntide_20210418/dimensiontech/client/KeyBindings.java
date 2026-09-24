package com.suntide_20210418.dimensiontech.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's key bindings. The chest marker analyses the block under the crosshair with a dedicated
 * key (default V) instead of right-click, because right-clicking a chest would open it rather than
 * let the item analyse it.
 */
public final class KeyBindings {
    /** Reuses the mod's existing key category (see the lang keys under this prefix). */
    public static final String CATEGORY = "key.categories.dimension_tech.main";

    public static final KeyMapping CHEST_ANALYSE =
            new KeyMapping(
                    "key.dimension_tech.chest_analyse",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    CATEGORY);

    private KeyBindings() {}
}
