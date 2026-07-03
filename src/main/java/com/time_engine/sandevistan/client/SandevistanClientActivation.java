package com.time_engine.sandevistan.client;

import com.time_engine.sandevistan.network.SandevistanActivationRequestPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SandevistanClientActivation {
    private SandevistanClientActivation() {}

    public static void requestToggle() {
        PacketDistributor.sendToServer(SandevistanActivationRequestPayload.INSTANCE);
    }
}
