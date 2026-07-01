package com.time_engine.engine.client;

import com.time_engine.engine.client.AfterimageTrail.RenderedAfterimage;
import com.time_engine.engine.common.network.AfterimagePayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class ClientAfterimageState {
    private static final Map<UUID, AfterimageTrail> TRAILS_BY_PLAYER = new HashMap<>();
    private static ResourceLocation currentDimension;

    private ClientAfterimageState() {}

    public static void apply(AfterimagePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canApply(minecraft, payload)) {
            return;
        }
        synchronizeDimension(minecraft);

        TRAILS_BY_PLAYER
                .computeIfAbsent(payload.state().entityId(), ignored -> new AfterimageTrail())
                .add(payload, minecraft.level.getGameTime());
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasActiveWorld(minecraft)) {
            clear();
            return;
        }
        synchronizeDimension(minecraft);

        long currentTick = minecraft.level.getGameTime();
        TRAILS_BY_PLAYER.values().forEach(trail -> trail.prune(currentTick));
        TRAILS_BY_PLAYER.values().removeIf(AfterimageTrail::isEmpty);
    }

    public static List<RenderedAfterimage> getRenderStates(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return List.of();
        }

        long currentTick = minecraft.level.getGameTime();
        return TRAILS_BY_PLAYER.values().stream()
                .flatMap(trail -> trail.getRenderStates(currentTick, partialTick).stream())
                .toList();
    }

    public static void clear() {
        TRAILS_BY_PLAYER.clear();
        currentDimension = null;
    }

    private static boolean canApply(Minecraft minecraft, AfterimagePayload payload) {
        if (!hasActiveWorld(minecraft)) {
            return false;
        }
        if (minecraft.player.getUUID().equals(payload.state().entityId())) {
            return false;
        }
        return minecraft.level.dimension().location().equals(payload.dimension());
    }

    private static boolean hasActiveWorld(Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.level != null;
    }

    private static void synchronizeDimension(Minecraft minecraft) {
        ResourceLocation dimension = minecraft.level.dimension().location();
        if (dimension.equals(currentDimension)) {
            return;
        }
        TRAILS_BY_PLAYER.clear();
        currentDimension = dimension;
    }
}
