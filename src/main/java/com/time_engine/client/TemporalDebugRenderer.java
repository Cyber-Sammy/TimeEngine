package com.time_engine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class TemporalDebugRenderer {
    private static final float GHOST_RED = 0.2F;
    private static final float GHOST_GREEN = 0.9F;
    private static final float GHOST_BLUE = 1.0F;
    private static final float GHOST_ALPHA = 0.85F;
    private static final float AFTERIMAGE_RED = 0.8F;
    private static final float AFTERIMAGE_GREEN = 0.3F;
    private static final float AFTERIMAGE_BLUE = 1.0F;

    private TemporalDebugRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<DebugBox> boxes = collectBoxes(partialTick);
        if (boxes.isEmpty()) {
            return;
        }

        renderBoxes(event, boxes);
    }

    private static List<DebugBox> collectBoxes(float partialTick) {
        List<DebugBox> boxes = new ArrayList<>();
        if (ClientTemporalState.isActive()) {
            ClientGhostState.getRenderStates(partialTick)
                    .forEach(
                            state ->
                                    boxes.add(
                                            new DebugBox(
                                                    state.boundingBox(),
                                                    GHOST_RED,
                                                    GHOST_GREEN,
                                                    GHOST_BLUE,
                                                    GHOST_ALPHA)));
        }
        ClientAfterimageState.getRenderStates(partialTick)
                .forEach(
                        afterimage ->
                                boxes.add(
                                        new DebugBox(
                                                afterimage.state().boundingBox(),
                                                AFTERIMAGE_RED,
                                                AFTERIMAGE_GREEN,
                                                AFTERIMAGE_BLUE,
                                                afterimage.alpha())));
        return boxes;
    }

    private static void renderBoxes(RenderLevelStageEvent event, List<DebugBox> boxes) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        VertexConsumer lines =
                minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        boxes.forEach(box -> box.render(poseStack, lines));
        poseStack.popPose();
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }

    private record DebugBox(AABB bounds, float red, float green, float blue, float alpha) {
        private void render(PoseStack poseStack, VertexConsumer lines) {
            LevelRenderer.renderLineBox(poseStack, lines, bounds, red, green, blue, alpha);
        }
    }
}
