package com.time_engine.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.time_engine.common.network.TemporalActivationRequestPayload;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.network.PacketDistributor;
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
        while (ACTIVATE.consumeClick()) {
            PacketDistributor.sendToServer(TemporalActivationRequestPayload.INSTANCE);
        }
    }
}
