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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EndgamePortalBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final float BLOCK_MIN = 0.0F;
    private static final float BLOCK_MAX = 1.0F;
    private static final float DEPTH_MIN = 0.002F;
    private static final float DEPTH_MAX = 0.998F;
    private static final float SKYBOX_SIZE = 96.0F;
    private static final float FIXED_SKYBOX_FOV = 70.0F;
    private static final float ITEM_WINDOW_SCALE = 0.985F;
    private static final List<Matrix4f> WINDOW_MASKS = new ArrayList<>();
    private static final List<Matrix4f> ITEM_WINDOW_MASKS = new ArrayList<>();
    private static final Set<Long> WINDOW_MASK_KEYS = new HashSet<>();
    private static final Set<MatrixKey> ITEM_WINDOW_MASK_KEYS = new HashSet<>();
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

        BlockPos blockPos = blockEntity.getBlockPos();
        if (!WINDOW_MASK_KEYS.add(blockPos.asLong())) {
            return;
        }

        Matrix4f maskPose = new Matrix4f(poseStack.last().pose());
        WINDOW_MASKS.add(maskPose);
        renderWindowDepthMask(maskPose);
    }

    public static void renderSkyboxLayer(RenderLevelStageEvent event) {
        renderSkyboxLayer(event, WINDOW_MASKS, WINDOW_MASK_KEYS);
    }

    public static void renderItemSkyboxLayer(RenderLevelStageEvent event) {
        renderSkyboxLayer(event, ITEM_WINDOW_MASKS, ITEM_WINDOW_MASK_KEYS);
    }

    private static void renderSkyboxLayer(RenderLevelStageEvent event, List<Matrix4f> queuedMasks, Set<?> queuedKeys) {
        if (!stencilEnabled) {
            ensureStencil(Minecraft.getInstance());
        }
        if (!stencilEnabled || queuedMasks.isEmpty()) {
            return;
        }

        List<Matrix4f> masks = new ArrayList<>(queuedMasks);
        queuedMasks.clear();
        queuedKeys.clear();

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
        BufferBuilder maskBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (Matrix4f mask : masks) {
            appendWindowMask(maskBuilder, mask, BLOCK_MIN, BLOCK_MAX);
        }
        BufferUploader.drawWithShader(maskBuilder.buildOrThrow());

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

    public static void registerWindowMask(Matrix4f pose) {
        Matrix4f itemPose = new Matrix4f(pose);
        itemPose.translate(0.5F, 0.5F, 0.5F);
        itemPose.scale(ITEM_WINDOW_SCALE);
        itemPose.translate(-0.5F, -0.5F, -0.5F);
        if (!ITEM_WINDOW_MASK_KEYS.add(MatrixKey.from(itemPose))) {
            return;
        }
        ITEM_WINDOW_MASKS.add(itemPose);
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

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        appendWindowMask(builder, pose, DEPTH_MIN, DEPTH_MAX);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void appendWindowMask(BufferBuilder builder, Matrix4f pose, float min, float max) {
        appendMaskFace(builder, pose, min, max, min, max, max, max, max, max);
        appendMaskFace(builder, pose, min, max, max, min, min, min, min, min);
        appendMaskFace(builder, pose, max, max, max, min, min, max, max, min);
        appendMaskFace(builder, pose, min, min, min, max, min, max, max, min);
        appendMaskFace(builder, pose, min, max, min, min, min, min, max, max);
        appendMaskFace(builder, pose, min, max, max, max, max, max, min, min);
    }

    private static void appendMaskFace(BufferBuilder builder, Matrix4f pose, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        builder.addVertex(pose, x0, y0, z0);
        builder.addVertex(pose, x1, y0, z1);
        builder.addVertex(pose, x1, y1, z2);
        builder.addVertex(pose, x0, y1, z3);
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

    private record MatrixKey(
            int m00, int m01, int m02, int m03,
            int m10, int m11, int m12, int m13,
            int m20, int m21, int m22, int m23,
            int m30, int m31, int m32, int m33
    ) {
        private static final float SCALE = 4096.0F;

        private static MatrixKey from(Matrix4f matrix) {
            return new MatrixKey(
                    quantize(matrix.m00()), quantize(matrix.m01()), quantize(matrix.m02()), quantize(matrix.m03()),
                    quantize(matrix.m10()), quantize(matrix.m11()), quantize(matrix.m12()), quantize(matrix.m13()),
                    quantize(matrix.m20()), quantize(matrix.m21()), quantize(matrix.m22()), quantize(matrix.m23()),
                    quantize(matrix.m30()), quantize(matrix.m31()), quantize(matrix.m32()), quantize(matrix.m33())
            );
        }

        private static int quantize(float value) {
            return Math.round(value * SCALE);
        }
    }
}
