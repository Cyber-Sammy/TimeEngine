package com.time_engine.api.client;

import com.time_engine.engine.client.ClientTemporalState;
import com.time_engine.engine.common.network.TemporalActivationRequestPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TemporalClientApi {
    private TemporalClientApi() {}

    public static void requestToggle() {
        PacketDistributor.sendToServer(TemporalActivationRequestPayload.INSTANCE);
    }

    public static TemporalClientStateView state() {
        return new TemporalClientStateView(
                ClientTemporalState.isActive(),
                ClientTemporalState.sessionId(),
                ClientTemporalState.activeTicksRemaining(),
                ClientTemporalState.cooldownTicksRemaining(),
                ClientTemporalState.timeScale(),
                ClientTemporalState.radius(),
                ClientTemporalState.phantomAttackReach());
    }
}
