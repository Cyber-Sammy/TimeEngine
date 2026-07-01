package com.time_engine.sandevistan.client;

import com.time_engine.engine.client.ClientTemporalState;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TemporalHud {
    private static final int ACTIVE_COLOR = 0xFF55FFFF;
    private static final int COOLDOWN_COLOR = 0xFFFFAA55;

    private TemporalHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        if (minecraft.player == null) {
            return;
        }

        Component text;
        int color;
        if (ClientTemporalState.isActive()) {
            text =
                    Component.translatable(
                            "hud.time_engine.active",
                            formatSeconds(ClientTemporalState.activeTicksRemaining()),
                            ClientTemporalState.timeScale());
            color = ACTIVE_COLOR;
        } else if (ClientTemporalState.cooldownTicksRemaining() > 0) {
            text =
                    Component.translatable(
                            "hud.time_engine.cooldown",
                            formatSeconds(ClientTemporalState.cooldownTicksRemaining()));
            color = COOLDOWN_COLOR;
        } else {
            return;
        }

        graphics.drawString(minecraft.font, text, 8, 8, color, true);
    }

    private static String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }
}
