package com.ddd.endgame.item.models;

import com.ddd.endgame.EndgameSkyboxItemRenderer;
import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.ModCompatibility;
import com.ddd.endgame.Xavitia;
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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class WeatherControllerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final WeatherControllerItemRenderer INSTANCE = new WeatherControllerItemRenderer();
    private static final ResourceLocation MASK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_weather_controller.png");
    private static List<EndgamePortalBlockEntityRenderer.MaskQuad> cachedMaskQuads;

    private WeatherControllerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ModCompatibility.beforeSkyboxItemRender(displayContext);
        BakedModel model = displayContext == ItemDisplayContext.GUI
                ? WeatherControllerModel.inventoryModel()
                : WeatherControllerModel.handModel();
        renderModel(model, stack, poseStack, buffer, packedLight, packedOverlay);
        flushItemBuffers(buffer);

        if (displayContext == ItemDisplayContext.GUI) {
            ModCompatibility.afterSkyboxItemRender(displayContext);
            return;
        }

        List<EndgamePortalBlockEntityRenderer.MaskQuad> quads = maskQuads();
        if (displayContext == ItemDisplayContext.GROUND) {
            EndgamePortalBlockEntityRenderer.registerMeshWindowMask(poseStack.last().pose(), quads, 1.0F);
            ModCompatibility.afterSkyboxItemRender(displayContext);
            return;
        }

        renderStencilWindow(displayContext, poseStack, quads);
        ModCompatibility.afterSkyboxItemRender(displayContext);
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

        BakedModel model = WeatherControllerModel.handModel();
        cachedMaskQuads = BakedStencilMaskQuads.build(MASK_TEXTURE, model, "Unable to build weather controller stencil mask");
        return cachedMaskQuads;
    }
}
