package com.ddd.endgame;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.RenderOptimizationCompat;
import com.mojang.blaze3d.platform.NativeImage;
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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class GeneratedStencilItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final int MASK_SIZE = 16;
    private static final float FRONT_Z = 7.5F / 16.0F;
    private static final float BACK_Z = 8.5F / 16.0F;
    private static final float CENTER_Z = 0.5F;

    private final ResourceLocation texture;
    private final String warningMessage;
    private final Function<ItemStack, Float> greenBlueSupplier;
    private PixelMasks cachedMasks;

    protected GeneratedStencilItemRenderer(ResourceLocation texture, Supplier<BakedModel> originalModel, String warningMessage) {
        this(texture, originalModel, warningMessage, stack -> 1.0F);
    }

    protected GeneratedStencilItemRenderer(ResourceLocation texture, Supplier<BakedModel> originalModel, String warningMessage, Function<ItemStack, Float> greenBlueSupplier) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.texture = texture;
        this.warningMessage = warningMessage;
        this.greenBlueSupplier = greenBlueSupplier;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        RenderOptimizationCompat.beforeSkyboxItemRender(displayContext);
        PixelMasks masks = pixelMasks();
        float greenBlue = greenBlueSupplier.apply(stack);
        renderFilteredGeneratedItem(poseStack.last().pose(), masks, greenBlue);
        flushItemBuffers(buffer);

        boolean[][] stencil = masks.stencil();
        if (displayContext == ItemDisplayContext.GROUND) {
            EndgamePortalBlockEntityRenderer.registerPixelWindowMask(poseStack.last().pose(), stencil, MASK_SIZE, FRONT_Z, BACK_Z, greenBlue);
            RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
            return;
        }

        renderStencilWindow(displayContext, poseStack, stencil, greenBlue);
        RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
    }

    private void renderFilteredGeneratedItem(Matrix4f pose, PixelMasks masks, float greenBlue) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int greenBlueColor = Mth.clamp(Math.round(greenBlue * 255.0F), 0, 255);
        float pixel = 1.0F / MASK_SIZE;
        for (int y = 0; y < MASK_SIZE; y++) {
            for (int x = 0; x < MASK_SIZE; x++) {
                if (!masks.visible()[y][x]) {
                    continue;
                }

                float minX = x * pixel;
                float maxX = minX + pixel;
                float maxY = 1.0F - y * pixel;
                float minY = maxY - pixel;
                float minU = x / (float) masks.width();
                float maxU = (x + 1) / (float) masks.width();
                float minV = y / (float) masks.height();
                float maxV = (y + 1) / (float) masks.height();
                addTexturedFrontBack(builder, pose, minX, minY, maxX, maxY, minU, minV, maxU, maxV, greenBlueColor);
                addVisibleEdges(builder, pose, masks.visible(), x, y, minX, minY, maxX, maxY, minU, minV, maxU, maxV, greenBlueColor);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.enableCull();
    }

    private static void flushItemBuffers(MultiBufferSource buffer) {
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
    }

    private static void renderStencilWindow(ItemDisplayContext displayContext, PoseStack poseStack, boolean[][] stencil, float greenBlue) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getMainRenderTarget().enableStencil();

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        renderMaskPixels(poseStack.last().pose(), stencil);

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, CENTER_Z);
            EndgamePortalBlockEntityRenderer.applyConfiguredSkyboxRotation(poseStack);
            EndgamePortalBlockEntityRenderer.renderSkyboxCube(poseStack.last().pose(), EndgameSkyboxItemRenderer.GUI_SKYBOX_SIZE, greenBlue);
            poseStack.popPose();
        } else {
            EndgamePortalBlockEntityRenderer.renderGlobalSkybox(greenBlue);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void renderMaskPixels(Matrix4f pose, boolean[][] stencil) {
        RenderSystem.disableCull();

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        float pixel = 1.0F / MASK_SIZE;
        for (int y = 0; y < MASK_SIZE; y++) {
            for (int x = 0; x < MASK_SIZE; x++) {
                if (!stencil[y][x]) {
                    continue;
                }

                float minX = x * pixel;
                float maxX = minX + pixel;
                float maxY = 1.0F - y * pixel;
                float minY = maxY - pixel;
                addDoubleSidedMaskQuad(builder, pose, minX, minY, maxX, maxY);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private PixelMasks pixelMasks() {
        if (cachedMasks != null) {
            return cachedMasks;
        }

        boolean[][] stencil = new boolean[MASK_SIZE][MASK_SIZE];
        boolean[][] visible = new boolean[MASK_SIZE][MASK_SIZE];
        int imageWidth = MASK_SIZE;
        int imageHeight = MASK_SIZE;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
                int width = Math.min(MASK_SIZE, image.getWidth());
                int height = Math.min(MASK_SIZE, image.getHeight());
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int alpha = pixel & 0xFF;
                        int red = pixel >>> 24;
                        int green = (pixel >>> 16) & 0xFF;
                        int blue = (pixel >>> 8) & 0xFF;
                        boolean redMarker = alpha > 0 && red == 255 && green == 0 && blue == 0;
                        stencil[y][x] = redMarker;
                        visible[y][x] = alpha > 0 && !redMarker;
                    }
                }
            } catch (IOException exception) {
                dddsendgame.LOGGER.warn(warningMessage, exception);
            }
        }

        cachedMasks = new PixelMasks(stencil, visible, imageWidth, imageHeight);
        return cachedMasks;
    }

    private static void addTexturedFrontBack(BufferBuilder builder, Matrix4f pose, float minX, float minY, float maxX, float maxY, float minU, float minV, float maxU, float maxV, int greenBlue) {
        builder.addVertex(pose, minX, minY, FRONT_Z).setUv(minU, maxV).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, maxX, minY, FRONT_Z).setUv(maxU, maxV).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, maxX, maxY, FRONT_Z).setUv(maxU, minV).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, minX, maxY, FRONT_Z).setUv(minU, minV).setColor(255, greenBlue, greenBlue, 255);

        builder.addVertex(pose, minX, maxY, BACK_Z).setUv(minU, minV).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, maxX, maxY, BACK_Z).setUv(maxU, minV).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, maxX, minY, BACK_Z).setUv(maxU, maxV).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, minX, minY, BACK_Z).setUv(minU, maxV).setColor(255, greenBlue, greenBlue, 255);
    }

    private static void addVisibleEdges(BufferBuilder builder, Matrix4f pose, boolean[][] visible, int x, int y, float minX, float minY, float maxX, float maxY, float minU, float minV, float maxU, float maxV, int greenBlue) {
        if (y == 0 || !visible[y - 1][x]) {
            addEdge(builder, pose, minX, maxY, FRONT_Z, maxX, maxY, FRONT_Z, maxX, maxY, BACK_Z, minX, maxY, BACK_Z, minU, minV, maxU, minV, greenBlue);
        }
        if (y == MASK_SIZE - 1 || !visible[y + 1][x]) {
            addEdge(builder, pose, minX, minY, BACK_Z, maxX, minY, BACK_Z, maxX, minY, FRONT_Z, minX, minY, FRONT_Z, minU, maxV, maxU, maxV, greenBlue);
        }
        if (x == 0 || !visible[y][x - 1]) {
            addEdge(builder, pose, minX, minY, BACK_Z, minX, minY, FRONT_Z, minX, maxY, FRONT_Z, minX, maxY, BACK_Z, minU, maxV, minU, minV, greenBlue);
        }
        if (x == MASK_SIZE - 1 || !visible[y][x + 1]) {
            addEdge(builder, pose, maxX, minY, FRONT_Z, maxX, minY, BACK_Z, maxX, maxY, BACK_Z, maxX, maxY, FRONT_Z, maxU, maxV, maxU, minV, greenBlue);
        }
    }

    private static void addEdge(BufferBuilder builder, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float u0, float v0, float u1, float v1, int greenBlue) {
        builder.addVertex(pose, x0, y0, z0).setUv(u0, v0).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, x1, y1, z1).setUv(u1, v1).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, x2, y2, z2).setUv(u1, v1).setColor(255, greenBlue, greenBlue, 255);
        builder.addVertex(pose, x3, y3, z3).setUv(u0, v0).setColor(255, greenBlue, greenBlue, 255);
    }

    private static void addDoubleSidedMaskQuad(BufferBuilder builder, Matrix4f pose, float minX, float minY, float maxX, float maxY) {
        builder.addVertex(pose, minX, minY, FRONT_Z);
        builder.addVertex(pose, maxX, minY, FRONT_Z);
        builder.addVertex(pose, maxX, maxY, FRONT_Z);
        builder.addVertex(pose, minX, maxY, FRONT_Z);

        builder.addVertex(pose, minX, maxY, BACK_Z);
        builder.addVertex(pose, maxX, maxY, BACK_Z);
        builder.addVertex(pose, maxX, minY, BACK_Z);
        builder.addVertex(pose, minX, minY, BACK_Z);
    }

    private record PixelMasks(boolean[][] stencil, boolean[][] visible, int width, int height) {
    }
}
