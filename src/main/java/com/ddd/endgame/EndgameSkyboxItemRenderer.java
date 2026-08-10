package com.ddd.endgame;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.RenderOptimizationCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class EndgameSkyboxItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final EndgameSkyboxItemRenderer INSTANCE = new EndgameSkyboxItemRenderer();
    static final float GUI_SKYBOX_SIZE = 0.95F;
    static final float LOCAL_SKYBOX_SIZE = 8.0F;

    private EndgameSkyboxItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        RenderOptimizationCompat.beforeSkyboxItemRender(displayContext);
        float greenBlue = GalaxyInstabilityVisuals.tintGreenBlue(stack);
        renderStencilWindow(displayContext, poseStack, greenBlue);
        renderBlockModel(blockItem.getBlock(), stack, poseStack, buffer, packedLight, packedOverlay);
        flushItemBuffers(buffer);
        RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
    }

    private static void renderBlockModel(Block block, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float greenBlue = GalaxyInstabilityVisuals.tintGreenBlue(stack);
        RenderSystem.setShaderColor(1.0F, greenBlue, greenBlue, 1.0F);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.renderSingleBlock(block.defaultBlockState(), poseStack, GalaxyInstabilityTint.wrap(buffer, greenBlue), packedLight, packedOverlay);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void flushItemBuffers(MultiBufferSource buffer) {
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
    }

    private static void renderStencilWindow(ItemDisplayContext displayContext, PoseStack poseStack, float greenBlue) {
        if (displayContext == ItemDisplayContext.GROUND) {
            EndgamePortalBlockEntityRenderer.registerWindowMask(poseStack.last().pose(), greenBlue);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getMainRenderTarget().enableStencil();

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        renderCube(poseStack.last().pose(), 0.0F, 1.0F);

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            EndgamePortalBlockEntityRenderer.applyConfiguredSkyboxRotation(poseStack);
            EndgamePortalBlockEntityRenderer.renderSkyboxCube(poseStack.last().pose(), GUI_SKYBOX_SIZE, greenBlue);
            poseStack.popPose();
        } else {
            EndgamePortalBlockEntityRenderer.renderGlobalSkybox(greenBlue);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void renderCube(Matrix4f pose, float min, float max) {
        renderFace(pose, min, max, min, max, max, max, max, max);
        renderFace(pose, min, max, max, min, min, min, min, min);
        renderFace(pose, max, max, max, min, min, max, max, min);
        renderFace(pose, min, min, min, max, min, max, max, min);
        renderFace(pose, min, max, min, min, min, min, max, max);
        renderFace(pose, min, max, max, max, max, max, min, min);
    }

    private static void renderFace(Matrix4f pose, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(pose, x0, y0, z0);
        builder.addVertex(pose, x1, y0, z1);
        builder.addVertex(pose, x1, y1, z2);
        builder.addVertex(pose, x0, y1, z3);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

}
