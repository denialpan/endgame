package com.ddd.endgame.item.models;

import com.ddd.endgame.Xevitia;
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

@EventBusSubscriber(modid = Xevitia.MODID, value = Dist.CLIENT)
public final class GeneratedStencilItemShader {
    private static ShaderInstance shader;
    private static final RenderStateShard.ShaderStateShard SHADER_STATE = new RenderStateShard.ShaderStateShard(GeneratedStencilItemShader::shader);
    private static final RenderType MAIN_TARGET_RENDER_TYPE = createRenderType(
            Xevitia.MODID + ":generated_stencil_item",
            RenderType.CompositeState.builder()
                    .setShaderState(SHADER_STATE)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true)
    );
    private static final RenderType ITEM_ENTITY_TARGET_RENDER_TYPE = createRenderType(
            Xevitia.MODID + ":generated_stencil_item_entity",
            RenderType.CompositeState.builder()
                    .setShaderState(SHADER_STATE)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true)
    );

    private static RenderType createRenderType(String name, RenderType.CompositeState state) {
        return RenderType.create(
            name,
            DefaultVertexFormat.BLOCK,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            1536,
            true,
            true,
            state
        );
    }

    private GeneratedStencilItemShader() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "generated_stencil_item"),
                        DefaultVertexFormat.BLOCK
                ),
                loadedShader -> shader = loadedShader
        );
    }

    public static ShaderInstance shader() {
        return shader;
    }

    public static RenderType renderType(boolean itemEntityTarget) {
        return itemEntityTarget ? ITEM_ENTITY_TARGET_RENDER_TYPE : MAIN_TARGET_RENDER_TYPE;
    }
}
