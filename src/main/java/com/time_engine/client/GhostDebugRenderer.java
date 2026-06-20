package com.time_engine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class GhostDebugRenderer {
    private static final float RED = 0.2F;
    private static final float GREEN = 0.9F;
    private static final float BLUE = 1.0F;
    private static final float ALPHA = 0.85F;

    private GhostDebugRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES
                || !ClientTemporalState.isActive()) {
            return;
        }

        var states =
                ClientGhostState.getRenderStates(
                        event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (states.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        VertexConsumer lines =
                minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        states.forEach(
                state ->
                        LevelRenderer.renderLineBox(
                                poseStack, lines, state.boundingBox(), RED, GREEN, BLUE, ALPHA));
        poseStack.popPose();
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }
}
