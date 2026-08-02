package com.ddd.endgame;

import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class EndgamePortalBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final float BLOCK_MIN = 0.0F;
    private static final float BLOCK_MAX = 1.0F;
    private static final float DEPTH_MIN = 0.002F;
    private static final float DEPTH_MAX = 0.998F;
    private static final float SKYBOX_SIZE = 96.0F;
    private static final float FIXED_SKYBOX_FOV = 70.0F;
    private static final List<Matrix4f> WINDOW_MASKS = new ArrayList<>();
    private static boolean stencilEnabled;

    public EndgamePortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureStencil(minecraft);
        if (!stencilEnabled) {
            return;
        }

        Matrix4f maskPose = new Matrix4f(poseStack.last().pose());
        WINDOW_MASKS.add(maskPose);
        renderWindowDepthMask(maskPose);
    }

    public static void renderSkyboxLayer(RenderLevelStageEvent event) {
        if (!stencilEnabled) {
            ensureStencil(Minecraft.getInstance());
        }
        if (!stencilEnabled || WINDOW_MASKS.isEmpty()) {
            return;
        }

        List<Matrix4f> masks = new ArrayList<>(WINDOW_MASKS);
        WINDOW_MASKS.clear();

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        PoseStack poseStack = event.getPoseStack();
        for (Matrix4f mask : masks) {
            renderWindowMask(mask, BLOCK_MIN, BLOCK_MAX);
        }

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        poseStack.pushPose();
        applyConfiguredSkyboxRotation(poseStack, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        withFixedSkyboxProjection(() -> renderSkyboxCube(poseStack.last().pose()));
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

    public static void applyConfiguredSkyboxRotation(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getTimer() == null) {
            applyConfiguredSkyboxRotation(poseStack, 0.0F);
            return;
        }

        applyConfiguredSkyboxRotation(poseStack, minecraft.getTimer().getGameTimeDeltaPartialTick(false));
    }

    public static void withFixedSkyboxProjection(Runnable renderAction) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(1, minecraft.getWindow().getWidth());
        int height = Math.max(1, minecraft.getWindow().getHeight());
        Matrix4f fixedProjection = new Matrix4f().perspective(
                (float)Math.toRadians(FIXED_SKYBOX_FOV),
                (float)width / (float)height,
                0.05F,
                minecraft.gameRenderer.getDepthFar()
        );

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(fixedProjection, VertexSorting.DISTANCE_TO_ORIGIN);
        try {
            renderAction.run();
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void applyConfiguredSkyboxRotation(PoseStack poseStack, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        double seconds = (minecraft.level.getGameTime() + partialTick) / 20.0D;
        double pitchSpeed = Config.SKYBOX_PITCH_ROTATION_SPEED.get();
        double yawSpeed = Config.SKYBOX_YAW_ROTATION_SPEED.get();
        double rollSpeed = Config.SKYBOX_ROLL_ROTATION_SPEED.get();

        if (pitchSpeed != 0.0D) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rotationDegrees(seconds, pitchSpeed)));
        }
        if (yawSpeed != 0.0D) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(seconds, yawSpeed)));
        }
        if (rollSpeed != 0.0D) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees(seconds, rollSpeed)));
        }
    }

    private static float rotationDegrees(double seconds, double degreesPerSecond) {
        return (float) ((seconds * degreesPerSecond) % 360.0D);
    }

    private static void renderWindowDepthMask(Matrix4f pose) {
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        renderWindowMask(pose, DEPTH_MIN, DEPTH_MAX);

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void renderWindowMask(Matrix4f pose, float min, float max) {
        renderMaskFace(pose, min, max, min, max, max, max, max, max);
        renderMaskFace(pose, min, max, max, min, min, min, min, min);
        renderMaskFace(pose, max, max, max, min, min, max, max, min);
        renderMaskFace(pose, min, min, min, max, min, max, max, min);
        renderMaskFace(pose, min, max, min, min, min, min, max, max);
        renderMaskFace(pose, min, max, max, max, max, max, min, min);
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
