package com.ddd.endgame;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = dddsendgame.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = dddsendgame.MODID, value = Dist.CLIENT)
public class dddsendgameClient {
    public dddsendgameClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        dddsendgame.LOGGER.info("HELLO FROM CLIENT SETUP");
        dddsendgame.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        NeoForge.EVENT_BUS.addListener(dddsendgameClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(dddsendgameClient::onRenderBlockHighlight);
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            EndgamePortalBlockEntityRenderer.renderItemSkyboxLayer(event);
            return;
        }

        Config.RenderStageMode mode = resolveRenderStageMode();
        if (mode == Config.RenderStageMode.DISABLED && event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            EndgamePortalBlockEntityRenderer.discardSkyboxLayer();
        } else if (mode == Config.RenderStageMode.VANILLA_STENCIL && event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            EndgamePortalBlockEntityRenderer.renderSkyboxLayer(event);
        } else if (mode == Config.RenderStageMode.IRIS_SAFE && event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            EndgamePortalBlockEntityRenderer.renderPhotonSkyboxLayer(event);
        }
    }

    private static Config.RenderStageMode resolveRenderStageMode() {
        Config.RenderStageMode configured = Config.IRIS_COMPATIBILITY_MODE.get();
        boolean shaderPackInUse = IrisCompat.isShaderPackInUse();
        if (!shaderPackInUse && configured == Config.RenderStageMode.IRIS_SAFE) {
            return Config.RenderStageMode.VANILLA_STENCIL;
        }
        if (configured == Config.RenderStageMode.AUTO) {
            return shaderPackInUse
                    ? Config.RenderStageMode.IRIS_SAFE
                    : Config.RenderStageMode.VANILLA_STENCIL;
        }
        return configured;
    }

    private static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (isEndgameSkyboxBlock(state)) {
            event.setCanceled(true);
        }
    }

    private static boolean isEndgameSkyboxBlock(BlockState state) {
        return state.is(dddsendgame.ENDGAME_CONTROLLER_BLOCK.get())
                || state.is(dddsendgame.ENDGAME_CONNECTOR_BLOCK.get())
                || state.is(dddsendgame.ENDGAME_SOLID_BLOCK.get())
                || state.is(dddsendgame.ENDGAME_GLASS_BLOCK.get())
                || state.is(dddsendgame.ENDGAME_FULL_GLASS_BLOCK.get());
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(dddsendgame.ENDGAME_CONTROLLER_MENU.get(), EndgameControllerScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_CONTROLLER_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_CONNECTOR_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_DECORATIVE_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
    }
}
