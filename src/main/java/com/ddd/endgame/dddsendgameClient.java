package com.ddd.endgame;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import com.ddd.endgame.block.EndgameDecorativeBlockEntity;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = dddsendgame.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = dddsendgame.MODID, value = Dist.CLIENT)
public class dddsendgameClient {
    private static final ModelResourceLocation WEATHER_CONTROLLER_HAND_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "item/weather_controller")
    );

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

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            GalaxyFreezerPreviewRenderer.render(event);
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
                || state.is(dddsendgame.ENDGAME_FULL_GLASS_BLOCK.get())
                || state.is(dddsendgame.GALAXY_BLOCK.get())
                || state.is(dddsendgame.GALAXY_FREEZER_BLOCK.get());
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(dddsendgame.ENDGAME_CONTROLLER_MENU.get(), EndgameControllerScreen::new);
        event.register(dddsendgame.GALAXY_FREEZER_MENU.get(), GalaxyFreezerScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_CONTROLLER_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_CONNECTOR_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dddsendgame.ENDGAME_DECORATIVE_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(dddsendgame.GALAXY_FREEZER_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null || tintIndex != 0) {
                return 0xFFFFFF;
            }
            if (level.getBlockEntity(pos) instanceof EndgameDecorativeBlockEntity blockEntity) {
                int greenBlue = Math.round(blockEntity.galaxyTintGreenBlue() * 255.0F);
                return 0xFF0000 | greenBlue << 8 | greenBlue;
            }
            return 0xFFFFFF;
        }, dddsendgame.GALAXY_BLOCK.get());
    }

    @SubscribeEvent
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WEATHER_CONTROLLER_HAND_MODEL);
    }

    @SubscribeEvent
    static void wrapGalaxyIngotModel(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation location = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "galaxy_ingot"));
        BakedModel original = event.getModels().get(location);
        if (original != null && !(original instanceof GalaxyIngotGeneratedModel)) {
            event.getModels().put(location, new GalaxyIngotGeneratedModel(original));
        }

        ModelResourceLocation dayLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "day_night_toggle"));
        BakedModel dayOriginal = event.getModels().get(dayLocation);
        if (dayOriginal != null && !(dayOriginal instanceof DayControllerModel)) {
            event.getModels().put(dayLocation, new DayControllerModel(dayOriginal));
        }

        ModelResourceLocation mobAnnihilatorLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "entity_purge_core"));
        BakedModel mobAnnihilatorOriginal = event.getModels().get(mobAnnihilatorLocation);
        if (mobAnnihilatorOriginal != null && !(mobAnnihilatorOriginal instanceof MobAnnihilatorModel)) {
            event.getModels().put(mobAnnihilatorLocation, new MobAnnihilatorModel(mobAnnihilatorOriginal));
        }

        ModelResourceLocation realityShifterLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "reality_restorer"));
        BakedModel realityShifterOriginal = event.getModels().get(realityShifterLocation);
        if (realityShifterOriginal != null && !(realityShifterOriginal instanceof RealityShifterModel)) {
            event.getModels().put(realityShifterLocation, new RealityShifterModel(realityShifterOriginal));
        }

        ModelResourceLocation freeFlightLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "survival_flight_core"));
        BakedModel freeFlightOriginal = event.getModels().get(freeFlightLocation);
        if (freeFlightOriginal != null && !(freeFlightOriginal instanceof FreeFlightModel)) {
            event.getModels().put(freeFlightLocation, new FreeFlightModel(freeFlightOriginal));
        }

        ModelResourceLocation noclipLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "spectator_phase_core"));
        BakedModel noclipOriginal = event.getModels().get(noclipLocation);
        if (noclipOriginal != null && !(noclipOriginal instanceof NoclipModel)) {
            event.getModels().put(noclipLocation, new NoclipModel(noclipOriginal));
        }

        ModelResourceLocation chunkAnnihilatorLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "chunk_annihilator"));
        BakedModel chunkAnnihilatorOriginal = event.getModels().get(chunkAnnihilatorLocation);
        if (chunkAnnihilatorOriginal != null && !(chunkAnnihilatorOriginal instanceof ChunkAnnihilatorModel)) {
            event.getModels().put(chunkAnnihilatorLocation, new ChunkAnnihilatorModel(chunkAnnihilatorOriginal));
        }

        ModelResourceLocation weatherLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "weather_cycler"));
        BakedModel weatherOriginal = event.getModels().get(weatherLocation);
        BakedModel weatherController = event.getModels().get(WEATHER_CONTROLLER_HAND_MODEL);
        if (weatherOriginal != null && !(weatherOriginal instanceof WeatherControllerModel)) {
            event.getModels().put(weatherLocation, new WeatherControllerModel(weatherOriginal, weatherController));
        }
    }
}
