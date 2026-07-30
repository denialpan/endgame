package com.ddd.endgame;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

public class EndgamePortalBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final float MIN = 0.002F;
    private static final float MAX = 0.998F;

    public EndgamePortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.endPortal());
        renderCube(pose, consumer);
    }

    private static void renderCube(Matrix4f pose, VertexConsumer consumer) {
        renderFace(pose, consumer, MIN, MAX, MIN, MAX, MAX, MAX, MAX, MAX);
        renderFace(pose, consumer, MIN, MAX, MAX, MIN, MIN, MIN, MIN, MIN);
        renderFace(pose, consumer, MAX, MAX, MAX, MIN, MIN, MAX, MAX, MIN);
        renderFace(pose, consumer, MIN, MIN, MIN, MAX, MIN, MAX, MAX, MIN);
        renderFace(pose, consumer, MIN, MAX, MIN, MIN, MIN, MIN, MAX, MAX);
        renderFace(pose, consumer, MIN, MAX, MAX, MAX, MAX, MAX, MIN, MIN);
    }

    private static void renderFace(
            Matrix4f pose,
            VertexConsumer consumer,
            float x0,
            float x1,
            float y0,
            float y1,
            float z0,
            float z1,
            float z2,
            float z3
    ) {
        consumer.addVertex(pose, x0, y0, z0);
        consumer.addVertex(pose, x1, y0, z1);
        consumer.addVertex(pose, x1, y1, z2);
        consumer.addVertex(pose, x0, y1, z3);
    }
}
