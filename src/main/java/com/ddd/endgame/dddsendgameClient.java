package com.ddd.endgame;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.compat.IrisCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import com.ddd.endgame.block.EndgameDecorativeBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = dddsendgame.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = dddsendgame.MODID, value = Dist.CLIENT)
public class dddsendgameClient {
    private static final ModelResourceLocation WEATHER_CONTROLLER_HAND_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "item/weather_controller")
    );
    private static Item lastGalaxyHotbarItem;
    private static Component lastGalaxyHotbarName = Component.empty();
    private static int galaxyHotbarNameTimer;

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
        NeoForge.EVENT_BUS.addListener(dddsendgameClient::onMouseScrolled);
        NeoForge.EVENT_BUS.addListener(dddsendgameClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(dddsendgameClient::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(dddsendgameClient::onRenderGuiLayerPre);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        GalaxyInstabilityVisuals.clientTick();
        updateGalaxyHotbarName();
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (event.getToolTip().isEmpty() || !usesGalaxyName(stack)) {
            return;
        }

        event.getToolTip().set(0, GalaxyTooltip.purpleWhite(stack.getHoverName().getString()));
    }

    private static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ItemStack selected = minecraft.player.getInventory().getSelected();
        if (!usesGalaxyName(selected) || galaxyHotbarNameTimer <= 0) {
            return;
        }

        event.setCanceled(true);
        renderGalaxyHotbarName(event, selected);
    }

    private static void renderGalaxyHotbarName(RenderGuiLayerEvent.Pre event, ItemStack selected) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Component name = GalaxyTooltip.purpleWhite(selected.getHoverName().getString());
        int width = font.width(name);
        int x = (event.getGuiGraphics().guiWidth() - width) / 2;
        int y = event.getGuiGraphics().guiHeight() - 59;
        if (minecraft.gameMode != null && !minecraft.gameMode.canHurtPlayer()) {
            y += 14;
        }

        int alpha = (int)((float)galaxyHotbarNameTimer * 256.0F / 10.0F);
        if (alpha > 255) {
            alpha = 255;
        }
        if (alpha <= 0) {
            return;
        }

        if (selected.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            name = name.copy().withStyle(ChatFormatting.ITALIC);
        }
        event.getGuiGraphics().drawStringWithBackdrop(font, name, x, y, width, FastColor.ARGB32.color(alpha, -1));
    }

    private static void updateGalaxyHotbarName() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            galaxyHotbarNameTimer = 0;
            lastGalaxyHotbarItem = null;
            lastGalaxyHotbarName = Component.empty();
            return;
        }

        ItemStack selected = minecraft.player.getInventory().getSelected();
        if (selected.isEmpty() || !usesGalaxyName(selected)) {
            galaxyHotbarNameTimer = 0;
            lastGalaxyHotbarItem = null;
            lastGalaxyHotbarName = Component.empty();
            return;
        }

        Component hoverName = selected.getHoverName();
        if (lastGalaxyHotbarItem == null || !selected.is(lastGalaxyHotbarItem) || !hoverName.equals(lastGalaxyHotbarName)) {
            galaxyHotbarNameTimer = (int)(40.0D * minecraft.options.notificationDisplayTime().get());
            lastGalaxyHotbarItem = selected.getItem();
            lastGalaxyHotbarName = hoverName;
        } else if (galaxyHotbarNameTimer > 0) {
            galaxyHotbarNameTimer--;
        }
    }

    private static boolean usesGalaxyName(ItemStack stack) {
        return stack.is(dddsendgame.WEATHER_CYCLER.get())
                || stack.is(dddsendgame.DAY_NIGHT_TOGGLE.get())
                || stack.is(dddsendgame.ENTITY_PURGE_CORE.get())
                || stack.is(dddsendgame.RANDOM_BLOCK_PLACER.get())
                || stack.is(dddsendgame.REALITY_RESTORER.get())
                || stack.is(dddsendgame.SURVIVAL_FLIGHT_CORE.get())
                || stack.is(dddsendgame.SPECTATOR_PHASE_CORE.get())
                || stack.is(dddsendgame.CHUNK_ANNIHILATOR.get())
                || stack.is(dddsendgame.GALAXY_INGOT.get())
                || stack.is(dddsendgame.GALAXY_BLOCK_ITEM.get())
                || stack.is(dddsendgame.ENDGAME_TEST_STICK.get());
    }

    private static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !minecraft.player.isShiftKeyDown()) {
            return;
        }
        if (!minecraft.player.getMainHandItem().is(dddsendgame.ENDGAME_TEST_STICK.get())
                && !minecraft.player.getOffhandItem().is(dddsendgame.ENDGAME_TEST_STICK.get())) {
            return;
        }

        double scroll = event.getScrollDeltaY();
        if (scroll == 0.0D) {
            return;
        }

        PacketDistributor.sendToServer(new EndgameTestStickModePayload(scroll > 0.0D ? 1 : -1));
        event.setCanceled(true);
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

        ModelResourceLocation endgameTestStickLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "endgame_test_stick"));
        BakedModel endgameTestStickOriginal = event.getModels().get(endgameTestStickLocation);
        if (endgameTestStickOriginal != null && !(endgameTestStickOriginal instanceof EndgameTestStickModel)) {
            event.getModels().put(endgameTestStickLocation, new EndgameTestStickModel(endgameTestStickOriginal));
        }

        ModelResourceLocation weatherLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "weather_cycler"));
        BakedModel weatherOriginal = event.getModels().get(weatherLocation);
        BakedModel weatherController = event.getModels().get(WEATHER_CONTROLLER_HAND_MODEL);
        if (weatherOriginal != null && !(weatherOriginal instanceof WeatherControllerModel)) {
            event.getModels().put(weatherLocation, new WeatherControllerModel(weatherOriginal, weatherController));
        }
    }
}
