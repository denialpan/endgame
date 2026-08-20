package com.ddd.endgame.item.models;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BakedStencilMaskQuads {
    private static final float MASK_DEPTH_OFFSET = 0.0005F;
    private static final float MASK_EDGE_OVERLAP = 0.0015F;

    private BakedStencilMaskQuads() {
    }

    public static List<EndgamePortalBlockEntityRenderer.MaskQuad> build(ResourceLocation texture, BakedModel model, String warningMessage) {
        if (model == null) {
            return List.of();
        }

        Optional<Resource> textureResource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (textureResource.isEmpty()) {
            return List.of();
        }

        List<EndgamePortalBlockEntityRenderer.MaskQuad> quads = new ArrayList<>();
        try (InputStream textureStream = textureResource.get().open(); NativeImage image = NativeImage.read(textureStream)) {
            RandomSource random = RandomSource.create(42L);
            collectBakedMaskQuads(quads, model, image, random);
            for (BakedModel renderPass : model.getRenderPasses(ItemStack.EMPTY, true)) {
                collectBakedMaskQuads(quads, renderPass, image, random);
            }
        } catch (IOException | IllegalStateException exception) {
            com.ddd.endgame.Xavitia.LOGGER.warn(warningMessage, exception);
        }

        return List.copyOf(quads);
    }

    private static void collectBakedMaskQuads(List<EndgamePortalBlockEntityRenderer.MaskQuad> maskQuads, BakedModel model, NativeImage image, RandomSource random) {
        random.setSeed(42L);
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
