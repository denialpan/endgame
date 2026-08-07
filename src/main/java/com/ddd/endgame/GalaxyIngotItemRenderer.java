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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
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
    private static final int MASK_SIZE = 16;
    private static final float FRONT_Z = 7.5F / 16.0F;
    private static final float BACK_Z = 8.5F / 16.0F;
    private static final float CENTER_Z = 0.5F;
    private static PixelMasks cachedMasks;

    private GalaxyIngotItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        RenderOptimizationCompat.beforeSkyboxItemRender(displayContext);
        renderOriginalGeneratedModel(stack, poseStack, buffer, packedLight, packedOverlay);
        flushItemBuffers(buffer);
        if (displayContext == ItemDisplayContext.GROUND) {
            EndgamePortalBlockEntityRenderer.registerPixelWindowMask(poseStack.last().pose(), pixelMasks().stencil(), MASK_SIZE, FRONT_Z, BACK_Z);
            RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
            return;
        }
        renderStencilWindow(displayContext, poseStack);
        RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
    }

    private static void renderOriginalGeneratedModel(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel model = GalaxyIngotGeneratedModel.originalModel();
        if (model == null) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        for (BakedModel renderPass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : renderPass.getRenderTypes(stack, true)) {
                itemRenderer.renderModelLists(renderPass, stack, packedLight, packedOverlay, poseStack, ItemRenderer.getFoilBufferDirect(buffer, renderType, true, stack.hasFoil()));
            }
        }
    }

    private static void flushItemBuffers(MultiBufferSource buffer) {
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
    }

    private static void renderStencilWindow(ItemDisplayContext displayContext, PoseStack poseStack) {
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
        renderMaskPixels(poseStack.last().pose());

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, CENTER_Z);
        EndgamePortalBlockEntityRenderer.applyConfiguredSkyboxRotation(poseStack);
        float skyboxSize = displayContext == ItemDisplayContext.GUI ? EndgameSkyboxItemRenderer.GUI_SKYBOX_SIZE : EndgameSkyboxItemRenderer.LOCAL_SKYBOX_SIZE;
        if (displayContext == ItemDisplayContext.GUI) {
            EndgamePortalBlockEntityRenderer.renderSkyboxCube(poseStack.last().pose(), skyboxSize);
        } else {
            EndgamePortalBlockEntityRenderer.withFixedSkyboxProjection(() -> EndgamePortalBlockEntityRenderer.renderSkyboxCube(poseStack.last().pose(), skyboxSize));
        }
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void renderMaskPixels(Matrix4f pose) {
        boolean[][] mask = pixelMasks().stencil();
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
                addDoubleSidedMaskQuad(builder, pose, minX, minY, maxX, maxY);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static PixelMasks pixelMasks() {
        if (cachedMasks != null) {
            return cachedMasks;
        }

        boolean[][] stencil = new boolean[MASK_SIZE][MASK_SIZE];
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(TEXTURE);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                int width = Math.min(MASK_SIZE, image.getWidth());
                int height = Math.min(MASK_SIZE, image.getHeight());
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int alpha = pixel & 0xFF;
                        int red = pixel >>> 24;
                        int green = (pixel >>> 16) & 0xFF;
                        int blue = (pixel >>> 8) & 0xFF;
                        if (alpha <= 0) {
                            continue;
                        }

                        boolean redMarker = red == 255 && green == 0 && blue == 0;
                        stencil[y][x] = redMarker;
                    }
                }
            } catch (IOException exception) {
                dddsendgame.LOGGER.warn("Unable to load galaxy ingot texture masks", exception);
            }
        }

        cachedMasks = new PixelMasks(stencil);
        return cachedMasks;
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

    private record PixelMasks(boolean[][] stencil) {
    }
}
