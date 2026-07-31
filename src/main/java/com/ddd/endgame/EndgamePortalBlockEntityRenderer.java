package com.ddd.endgame;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class EndgamePortalBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final float MIN = 0.002F;
    private static final float MAX = 0.998F;
    private static final float SKYBOX_SIZE = 96.0F;
    private static boolean stencilEnabled;
    private static boolean hasStencilMask;

    public EndgamePortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureStencil(minecraft);
        if (!stencilEnabled) {
            return;
        }

        renderWindowMask(blockEntity.getBlockPos(), minecraft.gameRenderer.getMainCamera().getPosition(), poseStack.last().pose());
    }

    public static void clearStencilMask() {
        if (!stencilEnabled) {
            ensureStencil(Minecraft.getInstance());
        }
        if (!stencilEnabled) {
            return;
        }

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        hasStencilMask = false;
    }

    public static void renderSkyboxLayer(RenderLevelStageEvent event) {
        if (!stencilEnabled || !hasStencilMask) {
            return;
        }

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        renderSkyboxCube(poseStack.last().pose());
        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void ensureStencil(Minecraft minecraft) {
        if (!stencilEnabled) {
            minecraft.getMainRenderTarget().enableStencil();
            stencilEnabled = minecraft.getMainRenderTarget().isStencilEnabled();
        }
    }

    private static void renderWindowMask(BlockPos blockPos, Vec3 cameraPos, Matrix4f pose) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        renderVisibleWindowFaces(blockPos, cameraPos, pose);

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        hasStencilMask = true;
    }

    private static void renderVisibleWindowFaces(BlockPos blockPos, Vec3 cameraPos, Matrix4f pose) {
        double cameraX = cameraPos.x - blockPos.getX();
        double cameraY = cameraPos.y - blockPos.getY();
        double cameraZ = cameraPos.z - blockPos.getZ();
        if (cameraZ >= MIN) {
            renderMaskFace(pose, MIN, MAX, MIN, MAX, MAX, MAX, MAX, MAX);
        }
        if (cameraZ <= MAX) {
            renderMaskFace(pose, MIN, MAX, MAX, MIN, MIN, MIN, MIN, MIN);
        }
        if (cameraX >= MIN) {
            renderMaskFace(pose, MAX, MAX, MAX, MIN, MIN, MAX, MAX, MIN);
        }
        if (cameraX <= MAX) {
            renderMaskFace(pose, MIN, MIN, MIN, MAX, MIN, MAX, MAX, MIN);
        }
        if (cameraY <= MAX) {
            renderMaskFace(pose, MIN, MAX, MIN, MIN, MIN, MIN, MAX, MAX);
        }
        if (cameraY >= MIN) {
            renderMaskFace(pose, MIN, MAX, MAX, MAX, MAX, MAX, MIN, MIN);
        }
    }

    private static void renderMaskFace(Matrix4f pose, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(pose, x0, y0, z0);
        builder.addVertex(pose, x1, y0, z1);
        builder.addVertex(pose, x1, y1, z2);
        builder.addVertex(pose, x0, y1, z3);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void renderSkyboxCube(Matrix4f pose) {
        float s = SKYBOX_SIZE;
        renderSkyboxFace(CubemapFace.FRONT, pose, -s, -s, s, s, -s, s, s, s, s, -s, s, s);
        renderSkyboxFace(CubemapFace.BACK, pose, s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s);
        renderSkyboxFace(CubemapFace.LEFT, pose, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s);
        renderSkyboxFace(CubemapFace.RIGHT, pose, s, -s, s, s, -s, -s, s, s, -s, s, s, s);
        renderSkyboxFace(CubemapFace.TOP, pose, -s, s, s, s, s, s, s, s, -s, -s, s, -s);
        renderSkyboxFace(CubemapFace.BOTTOM, pose, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s);
    }

    private static void renderSkyboxFace(
            CubemapFace face,
            Matrix4f pose,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3
    ) {
        RenderSystem.setShaderTexture(0, face.texture);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, x0, y0, z0).setUv(0.0F, 1.0F);
        builder.addVertex(pose, x1, y1, z1).setUv(1.0F, 1.0F);
        builder.addVertex(pose, x2, y2, z2).setUv(1.0F, 0.0F);
        builder.addVertex(pose, x3, y3, z3).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private enum CubemapFace {
        FRONT("front"),
        BACK("back"),
        LEFT("left"),
        RIGHT("right"),
        TOP("top"),
        BOTTOM("bottom");

        private final ResourceLocation texture;

        CubemapFace(String textureName) {
            this.texture = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/inner_skybox/" + textureName + ".png");
        }
    }
}
