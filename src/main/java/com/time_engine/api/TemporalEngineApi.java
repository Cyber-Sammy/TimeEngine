package com.time_engine.api;

import com.time_engine.engine.common.temporal.TemporalActivationService;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalEngineApi {
    private TemporalEngineApi() {}

    public static boolean toggleTemporalSession(ServerPlayer player) {
        return TemporalActivationService.toggle(player);
    }
}
