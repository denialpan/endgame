package com.ddd.endgame;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            EndgamePortalBlockEntityRenderer.clearStencilMask();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            EndgamePortalBlockEntityRenderer.renderSkyboxLayer(event);
        }
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(dddsendgame.ENDGAME_TEMPLATE_MENU.get(), EndgameTemplateScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_TEMPLATE_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_TEMPLATE_INPUT_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
    }
}
