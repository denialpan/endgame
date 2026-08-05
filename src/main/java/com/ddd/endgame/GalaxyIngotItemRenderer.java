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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class GalaxyIngotItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final GalaxyIngotItemRenderer INSTANCE = new GalaxyIngotItemRenderer();
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_ingot.png");
    private static final ResourceLocation MASK_TEXTURE = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_ingot_mask.png");
    private static final int MASK_SIZE = 16;
    private static boolean[][] cachedMask;

    private GalaxyIngotItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        RenderOptimizationCompat.beforeSkyboxItemRender(displayContext);
        renderStencilWindow(displayContext, poseStack);
        renderIngotTexture(poseStack.last().pose());
        RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
    }

    private static void renderStencilWindow(ItemDisplayContext displayContext, PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getMainRenderTarget().enableStencil();

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        renderMaskPixels(poseStack.last().pose());

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.0F);
        EndgamePortalBlockEntityRenderer.applyConfiguredSkyboxRotation(poseStack);
        float skyboxSize = displayContext == ItemDisplayContext.GUI ? EndgameSkyboxItemRenderer.GUI_SKYBOX_SIZE : EndgameSkyboxItemRenderer.LOCAL_SKYBOX_SIZE;
        if (displayContext == ItemDisplayContext.GUI) {
            EndgameSkyboxItemRenderer.renderSkyboxCube(poseStack.last().pose(), skyboxSize);
        } else {
            EndgamePortalBlockEntityRenderer.withFixedSkyboxProjection(() -> EndgameSkyboxItemRenderer.renderSkyboxCube(poseStack.last().pose(), skyboxSize));
        }
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void renderMaskPixels(Matrix4f pose) {
        boolean[][] mask = galaxyMask();
        RenderSystem.disableCull();

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        float pixel = 1.0F / MASK_SIZE;
        for (int y = 0; y < MASK_SIZE; y++) {
            for (int x = 0; x < MASK_SIZE; x++) {
                if (!mask[y][x]) {
                    continue;
                }

                float minX = x * pixel;
                float maxX = minX + pixel;
                float maxY = 1.0F - y * pixel;
                float minY = maxY - pixel;
                builder.addVertex(pose, minX, minY, 0.0F);
                builder.addVertex(pose, maxX, minY, 0.0F);
                builder.addVertex(pose, maxX, maxY, 0.0F);
                builder.addVertex(pose, minX, maxY, 0.0F);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static boolean[][] galaxyMask() {
        if (cachedMask != null) {
            return cachedMask;
        }

        boolean[][] mask = new boolean[MASK_SIZE][MASK_SIZE];
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MASK_TEXTURE);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                int width = Math.min(MASK_SIZE, image.getWidth());
                int height = Math.min(MASK_SIZE, image.getHeight());
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int alpha = pixel & 0xFF;
                        int red = pixel >>> 24;
                        mask[y][x] = alpha > 0 || red > 0;
                    }
                }
            } catch (IOException exception) {
                dddsendgame.LOGGER.warn("Unable to load galaxy ingot mask texture", exception);
                fillMask(mask);
            }
        } else {
            fillMask(mask);
        }

        cachedMask = mask;
        return cachedMask;
    }

    private static void fillMask(boolean[][] mask) {
        for (int y = 0; y < MASK_SIZE; y++) {
            for (int x = 0; x < MASK_SIZE; x++) {
                mask[y][x] = true;
            }
        }
    }

    private static void renderIngotTexture(Matrix4f pose) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, 0.0F, 1.0F, 0.0F).setUv(0.0F, 0.0F);
        builder.addVertex(pose, 1.0F, 1.0F, 0.0F).setUv(1.0F, 0.0F);
        builder.addVertex(pose, 1.0F, 0.0F, 0.0F).setUv(1.0F, 1.0F);
        builder.addVertex(pose, 0.0F, 0.0F, 0.0F).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
