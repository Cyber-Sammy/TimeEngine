package com.time_engine.sandevistan.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class TimeEngineKeyMappings {
    public static final KeyMapping ACTIVATE =
            new KeyMapping(
                    "key.time_engine.activate",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.time_engine");

    private TimeEngineKeyMappings() {}

    public static void handleInput() {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        while (ACTIVATE.consumeClick()) {
            SandevistanClientActivation.requestToggle();
        }
    }
}
