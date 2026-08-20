package com.ddd.endgame.item.models;

import com.ddd.endgame.EndgameSkyboxItemRenderer;
import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.ModCompatibility;
import com.ddd.endgame.Xavitia;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class GeneratedStencilItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final int MASK_SIZE = 16;
    private static final float FRONT_Z = 7.5F / 16.0F;
    private static final float BACK_Z = 8.5F / 16.0F;
    private static final float CENTER_Z = 0.5F;

    private final Function<ItemStack, ResourceLocation> texture;
    private final Function<ItemStack, BakedModel> originalModel;
    private final String warningMessage;
    private final boolean renderProcessedTextureModel;
    private final Map<ResourceLocation, PixelMasks> cachedMasks = new HashMap<>();

    protected GeneratedStencilItemRenderer(ResourceLocation texture, Supplier<BakedModel> originalModel, String warningMessage) {
        this(stack -> texture, stack -> originalModel.get(), warningMessage);
    }

    protected GeneratedStencilItemRenderer(ResourceLocation texture, Supplier<BakedModel> originalModel, String warningMessage, Function<ItemStack, Float> greenBlueSupplier) {
        this(stack -> texture, stack -> originalModel.get(), warningMessage);
    }

    protected GeneratedStencilItemRenderer(Function<ItemStack, ResourceLocation> texture, Function<ItemStack, BakedModel> originalModel, String warningMessage) {
        this(texture, originalModel, warningMessage, false);
    }

    protected GeneratedStencilItemRenderer(Function<ItemStack, ResourceLocation> texture, Function<ItemStack, BakedModel> originalModel, String warningMessage, Function<ItemStack, Float> greenBlueSupplier) {
        this(texture, originalModel, warningMessage, false);
    }

    protected GeneratedStencilItemRenderer(Function<ItemStack, ResourceLocation> texture, Function<ItemStack, BakedModel> originalModel, String warningMessage, Function<ItemStack, Float> greenBlueSupplier, boolean renderProcessedTextureModel) {
        this(texture, originalModel, warningMessage, renderProcessedTextureModel);
    }

    protected GeneratedStencilItemRenderer(Function<ItemStack, ResourceLocation> texture, Function<ItemStack, BakedModel> originalModel, String warningMessage, boolean renderProcessedTextureModel) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.texture = texture;
        this.originalModel = originalModel;
        this.warningMessage = warningMessage;
        this.renderProcessedTextureModel = renderProcessedTextureModel;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ModCompatibility.beforeSkyboxItemRender(displayContext);
        PixelMasks masks = pixelMasks(stack);
        float greenBlue = 1.0F;
        renderOriginalGeneratedModel(stack, displayContext, poseStack, buffer, packedLight, packedOverlay, greenBlue, masks);
        flushItemBuffers(buffer);

        boolean[][] stencil = masks.stencil();
        if (displayContext == ItemDisplayContext.GROUND) {
            EndgamePortalBlockEntityRenderer.registerPixelWindowMask(poseStack.last().pose(), stencil, MASK_SIZE, FRONT_Z, BACK_Z, greenBlue);
            ModCompatibility.afterSkyboxItemRender(displayContext);
            return;
        }

        renderStencilWindow(displayContext, poseStack, stencil, greenBlue);
        ModCompatibility.afterSkyboxItemRender(displayContext);
    }

    private void renderOriginalGeneratedModel(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float greenBlue, PixelMasks masks) {
        BakedModel model = originalModel.apply(stack);
        if (model == null) {
            return;
        }

        if (renderProcessedTextureModel || GeneratedStencilItemShader.shader() == null || ModCompatibility.isShaderPackInUse()) {
            renderProcessedTexturePixels(poseStack.last().pose(), this.texture.apply(stack), masks, greenBlue);
            return;
        }

        RenderSystem.setShaderColor(1.0F, greenBlue, greenBlue, 1.0F);
        RenderType renderType = GeneratedStencilItemShader.renderType(displayContext == ItemDisplayContext.GROUND);
        for (BakedModel renderPass : model.getRenderPasses(stack, true)) {
            renderModelLists(renderPass, stack, packedLight, packedOverlay, poseStack, buffer, greenBlue, renderType);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderVanillaModelLists(BakedModel model, ItemStack stack, int packedLight, int packedOverlay, PoseStack poseStack, MultiBufferSource buffer, float greenBlue) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        for (BakedModel renderPass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : renderPass.getRenderTypes(stack, true)) {
                itemRenderer.renderModelLists(
                        renderPass,
                        stack,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        ItemRenderer.getFoilBufferDirect(buffer, renderType, true, stack.hasFoil())
                );
            }
        }
    }

    private static void renderModelLists(BakedModel model, ItemStack stack, int packedLight, int packedOverlay, PoseStack poseStack, MultiBufferSource buffer, float greenBlue, RenderType renderType) {
        RandomSource randomSource = RandomSource.create();
        for (Direction direction : Direction.values()) {
            randomSource.setSeed(42L);
            renderQuadList(poseStack, buffer, model.getQuads(null, direction, randomSource), stack, packedLight, packedOverlay, greenBlue, renderType);
        }

        randomSource.setSeed(42L);
        renderQuadList(poseStack, buffer, model.getQuads(null, null, randomSource), stack, packedLight, packedOverlay, greenBlue, renderType);
    }

    private static void renderQuadList(PoseStack poseStack, MultiBufferSource buffer, java.util.List<BakedQuad> quads, ItemStack stack, int packedLight, int packedOverlay, float greenBlue, RenderType renderType) {
        ItemColors itemColors = Minecraft.getInstance().getItemColors();
        PoseStack.Pose pose = poseStack.last();
        var consumer = ItemRenderer.getFoilBufferDirect(buffer, renderType, true, stack.hasFoil());
        for (BakedQuad quad : quads) {
            int color = -1;
            if (!stack.isEmpty() && quad.isTinted()) {
                color = itemColors.getColor(stack, quad.getTintIndex());
            }

            float alpha = (float) FastColor.ARGB32.alpha(color) / 255.0F;
            float red = (float) FastColor.ARGB32.red(color) / 255.0F;
            float green = (float) FastColor.ARGB32.green(color) / 255.0F;
            float blue = (float) FastColor.ARGB32.blue(color) / 255.0F;
            consumer.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay, true);
        }
    }

    private static void renderProcessedTexturePixels(Matrix4f pose, ResourceLocation texture, PixelMasks masks, float greenBlue) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int[][] pixels = masks.pixels();
        boolean[][] stencil = masks.stencil();
        float pixelSize = 1.0F / MASK_SIZE;
        for (int y = 0; y < MASK_SIZE; y++) {
            for (int x = 0; x < MASK_SIZE; x++) {
                int pixel = pixels[y][x];
                int alpha = pixel & 0xFF;
                if (alpha <= 0 || stencil[y][x]) {
                    continue;
                }

                float minX = x * pixelSize;
                float maxX = minX + pixelSize;
                float maxY = 1.0F - y * pixelSize;
                float minY = maxY - pixelSize;
                float minU = x * pixelSize;
                float maxU = minU + pixelSize;
                float minV = y * pixelSize;
                float maxV = minV + pixelSize;
                addDoubleSidedTextureQuad(builder, pose, minX, minY, maxX, maxY, minU, minV, maxU, maxV, 255, 255, 255, 255);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
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
        if (ModCompatibility.isShaderPackInUse()) {
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            RenderSystem.disableBlend();
        }
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

        if (ModCompatibility.isShaderPackInUse()) {
            RenderSystem.enableBlend();
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

    private PixelMasks pixelMasks(ItemStack stack) {
        return pixelMasks(this.texture.apply(stack));
    }

    private PixelMasks pixelMasks(ResourceLocation texture) {
        PixelMasks cachedMask = cachedMasks.get(texture);
        if (cachedMask != null) {
            return cachedMask;
        }

        boolean[][] stencil = new boolean[MASK_SIZE][MASK_SIZE];
        int[][] pixels = new int[MASK_SIZE][MASK_SIZE];
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
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
                        pixels[y][x] = pixel;
                        stencil[y][x] = alpha > 0 && red == 255 && green == 0 && blue == 0;
                    }
                }
            } catch (IOException exception) {
                Xavitia.LOGGER.warn(warningMessage, exception);
            }
        }

        PixelMasks masks = new PixelMasks(stencil, pixels);
        cachedMasks.put(texture, masks);
        return masks;
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

    private static void addDoubleSidedTextureQuad(BufferBuilder builder, Matrix4f pose, float minX, float minY, float maxX, float maxY, float minU, float minV, float maxU, float maxV, int red, int green, int blue, int alpha) {
        builder.addVertex(pose, minX, minY, FRONT_Z).setUv(minU, maxV).setColor(red, green, blue, alpha);
        builder.addVertex(pose, maxX, minY, FRONT_Z).setUv(maxU, maxV).setColor(red, green, blue, alpha);
        builder.addVertex(pose, maxX, maxY, FRONT_Z).setUv(maxU, minV).setColor(red, green, blue, alpha);
        builder.addVertex(pose, minX, maxY, FRONT_Z).setUv(minU, minV).setColor(red, green, blue, alpha);

        builder.addVertex(pose, minX, maxY, BACK_Z).setUv(minU, minV).setColor(red, green, blue, alpha);
        builder.addVertex(pose, maxX, maxY, BACK_Z).setUv(maxU, minV).setColor(red, green, blue, alpha);
        builder.addVertex(pose, maxX, minY, BACK_Z).setUv(maxU, maxV).setColor(red, green, blue, alpha);
        builder.addVertex(pose, minX, minY, BACK_Z).setUv(minU, maxV).setColor(red, green, blue, alpha);
    }

    private record PixelMasks(boolean[][] stencil, int[][] pixels) {
    }
}
