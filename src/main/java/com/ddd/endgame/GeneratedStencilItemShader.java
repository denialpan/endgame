package com.ddd.endgame;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = dddsendgame.MODID, value = Dist.CLIENT)
public final class GeneratedStencilItemShader {
    private static ShaderInstance shader;
    private static final RenderStateShard.ShaderStateShard SHADER_STATE = new RenderStateShard.ShaderStateShard(GeneratedStencilItemShader::shader);
    private static final RenderType RENDER_TYPE = RenderType.create(
            dddsendgame.MODID + ":generated_stencil_item",
            DefaultVertexFormat.BLOCK,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            1536,
            true,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(SHADER_STATE)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true)
    );

    private GeneratedStencilItemShader() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "generated_stencil_item"),
                        DefaultVertexFormat.BLOCK
                ),
                loadedShader -> shader = loadedShader
        );
    }

    public static ShaderInstance shader() {
        return shader;
    }

    public static RenderType renderType() {
        return RENDER_TYPE;
    }
}
