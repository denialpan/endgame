package com.ddd.endgame;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.RenderOptimizationCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WeatherControllerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final WeatherControllerItemRenderer INSTANCE = new WeatherControllerItemRenderer();
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "models/item/weather_controller.json");
    private static final ResourceLocation MASK_TEXTURE = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_weather_controller.png");
    private static List<EndgamePortalBlockEntityRenderer.MaskQuad> cachedMaskQuads;

    private WeatherControllerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        RenderOptimizationCompat.beforeSkyboxItemRender(displayContext);
        renderOriginalModel(stack, poseStack, buffer, packedLight, packedOverlay);
        flushItemBuffers(buffer);

        List<EndgamePortalBlockEntityRenderer.MaskQuad> quads = maskQuads();
        if (displayContext == ItemDisplayContext.GROUND) {
            EndgamePortalBlockEntityRenderer.registerMeshWindowMask(poseStack.last().pose(), quads, 1.0F);
            RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
            return;
        }

        renderStencilWindow(displayContext, poseStack, quads);
        RenderOptimizationCompat.afterSkyboxItemRender(displayContext);
    }

    private static void renderOriginalModel(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel model = WeatherControllerModel.originalModel();
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
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> modelResource = minecraft.getResourceManager().getResource(MODEL);
        Optional<Resource> textureResource = minecraft.getResourceManager().getResource(MASK_TEXTURE);
        if (modelResource.isEmpty() || textureResource.isEmpty()) {
            cachedMaskQuads = List.of();
            return cachedMaskQuads;
        }

        try (
                InputStream modelStream = modelResource.get().open();
                InputStreamReader reader = new InputStreamReader(modelStream, StandardCharsets.UTF_8);
                InputStream textureStream = textureResource.get().open();
                NativeImage image = NativeImage.read(textureStream)
        ) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            if (elements != null) {
                for (JsonElement elementValue : elements) {
                    collectElementMaskQuads(quads, elementValue.getAsJsonObject(), image);
                }
            }
        } catch (IOException | IllegalStateException exception) {
            dddsendgame.LOGGER.warn("Unable to build weather controller stencil mask", exception);
        }

        cachedMaskQuads = List.copyOf(quads);
        return cachedMaskQuads;
    }

    private static void collectElementMaskQuads(List<EndgamePortalBlockEntityRenderer.MaskQuad> quads, JsonObject element, NativeImage image) {
        float[] from = vec3(element.getAsJsonArray("from"));
        float[] to = vec3(element.getAsJsonArray("to"));
        JsonObject rotation = element.has("rotation") ? element.getAsJsonObject("rotation") : null;
        JsonObject faces = element.getAsJsonObject("faces");
        if (faces == null) {
            return;
        }

        for (String direction : faces.keySet()) {
            JsonObject face = faces.getAsJsonObject(direction);
            if (!"#0".equals(face.get("texture").getAsString()) || !face.has("uv")) {
                continue;
            }
            collectFaceMaskQuads(quads, direction, from, to, rotation, uv(face.getAsJsonArray("uv")), image);
        }
    }

    private static void collectFaceMaskQuads(List<EndgamePortalBlockEntityRenderer.MaskQuad> quads, String direction, float[] from, float[] to, JsonObject rotation, float[] uv, NativeImage image) {
        float uMin = Math.min(uv[0], uv[2]);
        float uMax = Math.max(uv[0], uv[2]);
        float vMin = Math.min(uv[1], uv[3]);
        float vMax = Math.max(uv[1], uv[3]);
        int minX = clampPixel((int)Math.floor(uMin / 16.0F * image.getWidth()), image.getWidth());
        int maxX = clampPixel((int)Math.ceil(uMax / 16.0F * image.getWidth()), image.getWidth());
        int minY = clampPixel((int)Math.floor(vMin / 16.0F * image.getHeight()), image.getHeight());
        int maxY = clampPixel((int)Math.ceil(vMax / 16.0F * image.getHeight()), image.getHeight());

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (!isRedMarker(image.getPixelRGBA(x, y))) {
                    continue;
                }
                float u0 = x * 16.0F / image.getWidth();
                float u1 = (x + 1) * 16.0F / image.getWidth();
                float v0 = y * 16.0F / image.getHeight();
                float v1 = (y + 1) * 16.0F / image.getHeight();
                float s0 = normalized(u0, uv[0], uv[2]);
                float s1 = normalized(u1, uv[0], uv[2]);
                float t0 = normalized(v0, uv[1], uv[3]);
                float t1 = normalized(v1, uv[1], uv[3]);
                addFaceQuad(quads, direction, from, to, rotation, clamp01(s0), clamp01(t0), clamp01(s1), clamp01(t1));
            }
        }
    }

    private static void addFaceQuad(List<EndgamePortalBlockEntityRenderer.MaskQuad> quads, String direction, float[] from, float[] to, JsonObject rotation, float s0, float t0, float s1, float t1) {
        Vector3f p0 = facePoint(direction, from, to, s0, t1);
        Vector3f p1 = facePoint(direction, from, to, s1, t1);
        Vector3f p2 = facePoint(direction, from, to, s1, t0);
        Vector3f p3 = facePoint(direction, from, to, s0, t0);
        rotate(p0, rotation);
        rotate(p1, rotation);
        rotate(p2, rotation);
        rotate(p3, rotation);
        quads.add(new EndgamePortalBlockEntityRenderer.MaskQuad(
                p0.x, p0.y, p0.z,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z
        ));
    }

    private static Vector3f facePoint(String direction, float[] from, float[] to, float s, float t) {
        float minX = from[0] / 16.0F;
        float minY = from[1] / 16.0F;
        float minZ = from[2] / 16.0F;
        float maxX = to[0] / 16.0F;
        float maxY = to[1] / 16.0F;
        float maxZ = to[2] / 16.0F;
        float y = lerp(maxY, minY, t);

        return switch (direction) {
            case "north" -> new Vector3f(lerp(minX, maxX, s), y, minZ);
            case "south" -> new Vector3f(lerp(maxX, minX, s), y, maxZ);
            case "east" -> new Vector3f(maxX, y, lerp(minZ, maxZ, s));
            case "west" -> new Vector3f(minX, y, lerp(maxZ, minZ, s));
            case "up" -> new Vector3f(lerp(minX, maxX, s), maxY, lerp(maxZ, minZ, t));
            case "down" -> new Vector3f(lerp(minX, maxX, s), minY, lerp(minZ, maxZ, t));
            default -> new Vector3f();
        };
    }

    private static void rotate(Vector3f point, JsonObject rotation) {
        if (rotation == null) {
            return;
        }
        float angle = rotation.get("angle").getAsFloat();
        if (angle == 0.0F) {
            return;
        }
        float[] origin = vec3(rotation.getAsJsonArray("origin"));
        point.sub(origin[0] / 16.0F, origin[1] / 16.0F, origin[2] / 16.0F);
        switch (rotation.get("axis").getAsString()) {
            case "x" -> point.rotate(Axis.XP.rotationDegrees(angle));
            case "y" -> point.rotate(Axis.YP.rotationDegrees(angle));
            case "z" -> point.rotate(Axis.ZP.rotationDegrees(angle));
            default -> {
            }
        }
        point.add(origin[0] / 16.0F, origin[1] / 16.0F, origin[2] / 16.0F);
    }

    private static boolean isRedMarker(int pixel) {
        int alpha = pixel & 0xFF;
        int red = pixel >>> 24;
        int green = (pixel >>> 16) & 0xFF;
        int blue = (pixel >>> 8) & 0xFF;
        return alpha > 0 && red == 255 && green == 0 && blue == 0;
    }

    private static float[] vec3(JsonArray array) {
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private static float[] uv(JsonArray array) {
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat()};
    }

    private static float normalized(float value, float start, float end) {
        float length = end - start;
        return length == 0.0F ? 0.0F : (value - start) / length;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int clampPixel(int value, int size) {
        return Math.max(0, Math.min(size, value));
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }
}
