package com.time_engine.engine.client;

import com.time_engine.engine.client.ClientCausalLinkState.CausalLinkDebugView;
import com.time_engine.engine.config.TimeEngineClientConfig;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class CausalLinkDebugHud {
    private static final int NORMAL_COLOR = 0xFFFF5533;
    private static final int STALE_COLOR = 0xFFFFAA55;
    private static final int TEXT_SHADOW_COLOR = 0xAA000000;

    private CausalLinkDebugHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canRender(minecraft)) {
            return;
        }

        List<CausalLinkDebugView> views = ClientCausalLinkState.getDebugViews();
        if (views.isEmpty()) {
            return;
        }

        List<CausalLinkDebugView> sortedViews =
                views.stream().sorted(Comparator.comparing(CausalLinkDebugView::ownerId)).toList();
        renderCornerDiagnostics(graphics, minecraft, sortedViews);
        renderBottomSummary(graphics, minecraft, primaryView(minecraft, sortedViews));
    }

    private static boolean canRender(Minecraft minecraft) {
        if (!TimeEngineClientConfig.showCausalDebugHud()) {
            return false;
        }
        if (minecraft.options.hideGui) {
            return false;
        }
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.level != null;
    }

    private static void renderCornerDiagnostics(
            GuiGraphics graphics, Minecraft minecraft, List<CausalLinkDebugView> views) {
        int y = 28;
        drawLine(graphics, minecraft, "Causal links: " + views.size(), 8, y, NORMAL_COLOR);
        y += 10;

        int limit = Math.min(views.size(), 3);
        for (int index = 0; index < limit; index++) {
            CausalLinkDebugView view = views.get(index);
            int color = colorFor(view);
            drawLine(graphics, minecraft, roleLine(minecraft, view), 8, y, color);
            y += 10;
            drawLine(graphics, minecraft, progressLine(view), 8, y, color);
            y += 10;
            drawLine(graphics, minecraft, distanceLine(view), 8, y, color);
            y += 12;
        }
    }

    private static void renderBottomSummary(
            GuiGraphics graphics, Minecraft minecraft, CausalLinkDebugView view) {
        String text =
                String.format(
                        Locale.ROOT,
                        "Causal %s | %s -> %s | progress %.0f%% | stale %dt",
                        role(minecraft, view),
                        nameOrShortId(minecraft, view.ownerId(), view.shortOwnerId()),
                        nameOrShortId(minecraft, view.targetId(), view.shortTargetId()),
                        view.progress() * 100.0D,
                        view.staleTicks());
        int width = minecraft.font.width(text);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 58;
        drawLine(graphics, minecraft, text, x, y, colorFor(view));
    }

    private static CausalLinkDebugView primaryView(
            Minecraft minecraft, List<CausalLinkDebugView> views) {
        UUID localPlayerId = minecraft.player.getUUID();
        return views.stream()
                .filter(view -> view.targetId().equals(localPlayerId))
                .findFirst()
                .orElse(views.getFirst());
    }

    private static String roleLine(Minecraft minecraft, CausalLinkDebugView view) {
        return String.format(
                Locale.ROOT,
                "%s %s -> %s [%s]",
                role(minecraft, view),
                nameOrShortId(minecraft, view.ownerId(), view.shortOwnerId()),
                nameOrShortId(minecraft, view.targetId(), view.shortTargetId()),
                view.state());
    }

    private static String progressLine(CausalLinkDebugView view) {
        return String.format(
                Locale.ROOT,
                "progress=%.3f serverTick=%d stale=%dt",
                view.progress(),
                view.serverTick(),
                view.staleTicks());
    }

    private static String distanceLine(CausalLinkDebugView view) {
        return String.format(
                Locale.ROOT,
                "pursuitToTarget=%.2f targetFromOrigin=%.2f",
                view.pursuitDistanceToTarget(),
                view.targetDistanceFromOrigin());
    }

    private static String role(Minecraft minecraft, CausalLinkDebugView view) {
        UUID localPlayerId = minecraft.player.getUUID();
        if (view.ownerId().equals(localPlayerId)) {
            return "OWNER";
        }
        if (view.targetId().equals(localPlayerId)) {
            return "TARGET";
        }
        return "OBSERVER";
    }

    private static int colorFor(CausalLinkDebugView view) {
        return view.stale() ? STALE_COLOR : NORMAL_COLOR;
    }

    private static String nameOrShortId(Minecraft minecraft, UUID playerId, String fallback) {
        PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerId);
        if (info == null) {
            return fallback;
        }
        Component tabName = info.getTabListDisplayName();
        if (tabName != null) {
            return tabName.getString();
        }
        return info.getProfile().getName();
    }

    private static void drawLine(
            GuiGraphics graphics, Minecraft minecraft, String text, int x, int y, int color) {
        graphics.fill(
                x - 2,
                y - 1,
                x + minecraft.font.width(text) + 2,
                y + minecraft.font.lineHeight,
                TEXT_SHADOW_COLOR);
        graphics.drawString(minecraft.font, text, x, y, color, true);
    }
}
