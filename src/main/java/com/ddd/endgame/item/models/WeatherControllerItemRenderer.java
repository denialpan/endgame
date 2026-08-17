package com.ddd.endgame.item.models;

import com.ddd.endgame.EndgameSkyboxItemRenderer;
import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.RenderOptimizationCompat;
import com.ddd.endgame.Xevitia;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WeatherControllerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final WeatherControllerItemRenderer INSTANCE = new WeatherControllerItemRenderer();
    private static final ResourceLocation MASK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "textures/item/galaxy_weather_controller.png");
    private static final float MASK_DEPTH_OFFSET = 0.0005F;
    private static final float MASK_EDGE_OVERLAP = 0.0015F;
    private static List<EndgamePortalBlockEntityRenderer.MaskQuad> cachedMaskQuads;

    private WeatherControllerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        RenderOptimizationCompat.beforeSkyboxItemRender(displayContext);
        BakedModel model = displayContext == ItemDisplayContext.GUI
                ? WeatherControllerModel.inventoryModel()
                : WeatherControllerModel.handModel();
        renderModel(model, stack, poseStack, buffer, packedLight, packedOverlay);
        flushItemBuffers(buffer);

        if (displayContext == ItemDisplayContext.GUI) {
            RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
            return;
        }

        List<EndgamePortalBlockEntityRenderer.MaskQuad> quads = maskQuads();
        if (displayContext == ItemDisplayContext.GROUND) {
            EndgamePortalBlockEntityRenderer.registerMeshWindowMask(poseStack.last().pose(), quads, 1.0F);
            RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
            return;
        }

        renderStencilWindow(displayContext, poseStack, quads);
        RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
    }

    private static void renderModel(BakedModel model, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (model == null) {
            return;
        }

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

    private static void flushItemBuffers(MultiBufferSource buffer) {
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
    }

    private static void renderStencilWindow(ItemDisplayContext displayContext, PoseStack poseStack, List<EndgamePortalBlockEntityRenderer.MaskQuad> quads) {
        if (quads.isEmpty()) {
            return;
        }

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

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        Matrix4f pose = poseStack.last().pose();
        for (EndgamePortalBlockEntityRenderer.MaskQuad quad : quads) {
            EndgamePortalBlockEntityRenderer.appendMaskQuad(builder, pose, quad);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());

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
            EndgamePortalBlockEntityRenderer.renderSkyboxCube(poseStack.last().pose(), EndgameSkyboxItemRenderer.GUI_SKYBOX_SIZE);
            poseStack.popPose();
        } else {
            EndgamePortalBlockEntityRenderer.renderGlobalSkybox();
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

    private static List<EndgamePortalBlockEntityRenderer.MaskQuad> maskQuads() {
        if (cachedMaskQuads != null) {
            return cachedMaskQuads;
        }

        List<EndgamePortalBlockEntityRenderer.MaskQuad> quads = new ArrayList<>();
        BakedModel model = WeatherControllerModel.handModel();
        if (model == null) {
            cachedMaskQuads = List.of();
            return cachedMaskQuads;
        }

        Optional<Resource> textureResource = Minecraft.getInstance().getResourceManager().getResource(MASK_TEXTURE);
        if (textureResource.isEmpty()) {
            cachedMaskQuads = List.of();
            return cachedMaskQuads;
        }

        try (InputStream textureStream = textureResource.get().open(); NativeImage image = NativeImage.read(textureStream)) {
            RandomSource random = RandomSource.create(42L);
            collectBakedMaskQuads(quads, model, image, random);
            for (BakedModel renderPass : model.getRenderPasses(ItemStack.EMPTY, true)) {
                collectBakedMaskQuads(quads, renderPass, image, random);
            }
        } catch (IOException | IllegalStateException exception) {
            Xevitia.LOGGER.warn("Unable to build weather compressor stencil mask", exception);
        }

        cachedMaskQuads = List.copyOf(quads);
        return cachedMaskQuads;
    }

    private static void collectBakedMaskQuads(List<EndgamePortalBlockEntityRenderer.MaskQuad> maskQuads, BakedModel model, NativeImage image, RandomSource random) {
        collectBakedMaskQuads(maskQuads, model.getQuads(null, null, random, ModelData.EMPTY, null), image);
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            collectBakedMaskQuads(maskQuads, model.getQuads(null, direction, random, ModelData.EMPTY, null), image);
        }
    }

    private static void collectBakedMaskQuads(List<EndgamePortalBlockEntityRenderer.MaskQuad> maskQuads, List<BakedQuad> bakedQuads, NativeImage image) {
        for (BakedQuad bakedQuad : bakedQuads) {
            collectBakedMaskQuad(maskQuads, bakedQuad, image);
        }
    }

    private static void collectBakedMaskQuad(List<EndgamePortalBlockEntityRenderer.MaskQuad> maskQuads, BakedQuad bakedQuad, NativeImage image) {
        QuadVertex[] vertices = vertices(bakedQuad);
        float minU = Math.min(Math.min(vertices[0].u, vertices[1].u), Math.min(vertices[2].u, vertices[3].u));
        float maxU = Math.max(Math.max(vertices[0].u, vertices[1].u), Math.max(vertices[2].u, vertices[3].u));
        float minV = Math.min(Math.min(vertices[0].v, vertices[1].v), Math.min(vertices[2].v, vertices[3].v));
        float maxV = Math.max(Math.max(vertices[0].v, vertices[1].v), Math.max(vertices[2].v, vertices[3].v));
        int minX = clampPixel((int)Math.floor(minU * image.getWidth()), image.getWidth());
        int maxX = clampPixel((int)Math.ceil(maxU * image.getWidth()), image.getWidth());
        int minY = clampPixel((int)Math.floor(minV * image.getHeight()), image.getHeight());
        int maxY = clampPixel((int)Math.ceil(maxV * image.getHeight()), image.getHeight());

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (!isRedMarker(image.getPixelRGBA(x, y))) {
                    continue;
                }

                float u0 = x / (float)image.getWidth();
                float u1 = (x + 1) / (float)image.getWidth();
                float v0 = y / (float)image.getHeight();
                float v1 = (y + 1) / (float)image.getHeight();
                Vector3f p0 = positionForUv(vertices, u0, v1);
                Vector3f p1 = positionForUv(vertices, u1, v1);
                Vector3f p2 = positionForUv(vertices, u1, v0);
                Vector3f p3 = positionForUv(vertices, u0, v0);
                if (p0 == null || p1 == null || p2 == null || p3 == null) {
                    continue;
                }

                expandInPlaneSlightly(p0, p1, p2, p3);
                offsetSlightly(p0, p1, p2, p3);
                maskQuads.add(new EndgamePortalBlockEntityRenderer.MaskQuad(
                        p0.x, p0.y, p0.z,
                        p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z,
                        p3.x, p3.y, p3.z
                ));
            }
        }
    }

    private static QuadVertex[] vertices(BakedQuad bakedQuad) {
        int[] packed = bakedQuad.getVertices();
        TextureAtlasSprite sprite = bakedQuad.getSprite();
        QuadVertex[] vertices = new QuadVertex[4];
        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            vertices[i] = new QuadVertex(
                    Float.intBitsToFloat(packed[offset]),
                    Float.intBitsToFloat(packed[offset + 1]),
                    Float.intBitsToFloat(packed[offset + 2]),
                    clamp01(sprite.getUOffset(Float.intBitsToFloat(packed[offset + 4]))),
                    clamp01(sprite.getVOffset(Float.intBitsToFloat(packed[offset + 5])))
            );
        }
        return vertices;
    }

    private static Vector3f positionForUv(QuadVertex[] vertices, float u, float v) {
        Vector3f position = positionInTriangle(vertices[0], vertices[1], vertices[2], u, v);
        if (position != null) {
            return position;
        }
        return positionInTriangle(vertices[0], vertices[2], vertices[3], u, v);
    }

    private static Vector3f positionInTriangle(QuadVertex a, QuadVertex b, QuadVertex c, float u, float v) {
        float v0u = b.u - a.u;
        float v0v = b.v - a.v;
        float v1u = c.u - a.u;
        float v1v = c.v - a.v;
        float v2u = u - a.u;
        float v2v = v - a.v;
        float denominator = v0u * v1v - v1u * v0v;
        if (Math.abs(denominator) < 0.000001F) {
            return null;
        }

        float s = (v2u * v1v - v1u * v2v) / denominator;
        float t = (v0u * v2v - v2u * v0v) / denominator;
        if (s < -0.001F || t < -0.001F || s + t > 1.001F) {
            return null;
        }

        float w = 1.0F - s - t;
        return new Vector3f(
                a.x * w + b.x * s + c.x * t,
                a.y * w + b.y * s + c.y * t,
                a.z * w + b.z * s + c.z * t
        );
    }

    private static void expandInPlaneSlightly(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
        Vector3f u = new Vector3f(p1).sub(p0);
        Vector3f v = new Vector3f(p3).sub(p0);
        if (u.lengthSquared() <= 0.0000001F || v.lengthSquared() <= 0.0000001F) {
            return;
        }

        u.normalize().mul(MASK_EDGE_OVERLAP);
        v.normalize().mul(MASK_EDGE_OVERLAP);
        p0.sub(u).sub(v);
        p1.add(u).sub(v);
        p2.add(u).add(v);
        p3.sub(u).add(v);
    }

    private static void offsetSlightly(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
        Vector3f normal = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0));
        if (normal.lengthSquared() <= 0.0000001F) {
            return;
        }
        normal.normalize().mul(MASK_DEPTH_OFFSET);
        p0.add(normal);
        p1.add(normal);
        p2.add(normal);
        p3.add(normal);
    }

    private static boolean isRedMarker(int pixel) {
        int alpha = pixel & 0xFF;
        int red = pixel >>> 24;
        int green = (pixel >>> 16) & 0xFF;
        int blue = (pixel >>> 8) & 0xFF;
        return alpha > 0 && red == 255 && green == 0 && blue == 0;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int clampPixel(int value, int size) {
        return Math.max(0, Math.min(size, value));
    }

    private record QuadVertex(float x, float y, float z, float u, float v) {
    }
}
