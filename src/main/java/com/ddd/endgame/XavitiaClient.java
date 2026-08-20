package com.ddd.endgame;

import com.ddd.endgame.galaxy.GalaxyTooltip;

import com.ddd.endgame.galaxy.GalaxyFreezerScreen;

import com.ddd.endgame.galaxy.GalaxyFreezerPreviewRenderer;

import com.ddd.endgame.galaxy.GalaxyCompressorScreen;

import com.ddd.endgame.block.EndgamePortalBlockEntityRenderer;
import com.ddd.endgame.block.GalaxyFreezerBlockEntity;
import com.ddd.endgame.compat.ModCompatibility;
import com.ddd.endgame.item.GalaxyToolItem;
import com.ddd.endgame.item.GalaxyMultitoolItem;
import com.ddd.endgame.item.ItemFabricatorItem;
import com.ddd.endgame.item.models.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ddd.endgame.payload.ItemFabricatorSelectionPayload;
import com.ddd.endgame.payload.GalaxyMultitoolSelectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Xavitia.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Xavitia.MODID, value = Dist.CLIENT)
public class XavitiaClient {
    private static final ModelResourceLocation WEATHER_CONTROLLER_HAND_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "item/weather_controller")
    );
    private static Item lastGalaxyHotbarItem;
    private static Component lastGalaxyHotbarName = Component.empty();
    private static int galaxyHotbarNameTimer;

    public XavitiaClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Xavitia.LOGGER.info("HELLO FROM CLIENT SETUP");
        Xavitia.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        NeoForge.EVENT_BUS.addListener(XavitiaClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(XavitiaClient::onRenderBlockHighlight);
        NeoForge.EVENT_BUS.addListener(XavitiaClient::onMouseScrolled);
        NeoForge.EVENT_BUS.addListener(XavitiaClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(XavitiaClient::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(XavitiaClient::onRenderGuiLayerPre);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        updateGalaxyHotbarName();
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (GalaxyFreezerBlockEntity.isCoolant(stack)) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.xavitia.cooling_time",
                    Component.literal(String.valueOf(GalaxyFreezerBlockEntity.coolingPeriodSeconds(stack))).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            ).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        if (event.getToolTip().isEmpty() || !usesGalaxyName(stack)) {
            return;
        }

        event.getToolTip().set(0, GalaxyTooltip.purpleWhite(stack.getHoverName().getString(), stack.is(Xavitia.THE_STICK.get())));
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
        Component name = GalaxyTooltip.purpleWhite(selected.getHoverName().getString(), selected.is(Xavitia.THE_STICK.get()));
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
        } else if (!minecraft.isPaused() && galaxyHotbarNameTimer > 0) {
            galaxyHotbarNameTimer--;
        }
    }

    private static boolean usesGalaxyName(ItemStack stack) {
        return stack.is(Xavitia.WEATHER_CYCLER.get())
                || stack.is(Xavitia.DAY_NIGHT_TOGGLE.get())
                || stack.is(Xavitia.MOB_ANNIHILATOR.get())
                || stack.is(Xavitia.ITEMFABRICATOR.get())
                || stack.is(Xavitia.GALAXY_MULTITOOL.get())
                || stack.is(Xavitia.GALAXY_PICKAXE.get())
                || stack.is(Xavitia.GALAXY_AXE.get())
                || stack.is(Xavitia.GALAXY_HOE.get())
                || stack.is(Xavitia.GALAXY_SHOVEL.get())
                || stack.is(Xavitia.GALAXY_SWORD.get())
                || stack.is(Xavitia.REALITY_RESTORER.get())
                || stack.is(Xavitia.SURVIVAL_FLIGHT_CORE.get())
                || stack.is(Xavitia.SPECTATOR_PHASE_CORE.get())
                || stack.is(Xavitia.CHUNK_DESTROYER.get())
                || stack.is(Xavitia.GALAXY_INGOT.get())
                || stack.is(Xavitia.GALAXY_BLOCK_ITEM.get())
                || stack.is(Xavitia.THE_STICK.get());
    }

    private static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !minecraft.player.isShiftKeyDown()) {
            return;
        }

        double scroll = event.getScrollDeltaY();
        if (scroll == 0.0D) {
            return;
        }

        int direction = scroll > 0.0D ? 1 : -1;
        if (minecraft.player.getMainHandItem().is(Xavitia.ITEMFABRICATOR.get())
                || minecraft.player.getOffhandItem().is(Xavitia.ITEMFABRICATOR.get())) {
            ItemStack fabricator = minecraft.player.getMainHandItem().is(Xavitia.ITEMFABRICATOR.get())
                    ? minecraft.player.getMainHandItem()
                    : minecraft.player.getOffhandItem();
            int fabricatorDirection = -direction;
            ItemFabricatorItem.cycleSelectedItem(fabricator, fabricatorDirection);
            PacketDistributor.sendToServer(new ItemFabricatorSelectionPayload(fabricatorDirection));
            event.setCanceled(true);
        }

        if (minecraft.player.getMainHandItem().is(Xavitia.GALAXY_MULTITOOL.get())
                || minecraft.player.getOffhandItem().is(Xavitia.GALAXY_MULTITOOL.get())) {
            ItemStack multitool = minecraft.player.getMainHandItem().is(Xavitia.GALAXY_MULTITOOL.get())
                    ? minecraft.player.getMainHandItem()
                    : minecraft.player.getOffhandItem();
            GalaxyMultitoolItem.cycleSelectedTool(multitool, direction);
            PacketDistributor.sendToServer(new GalaxyMultitoolSelectionPayload(direction));
            event.setCanceled(true);
        }
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
        boolean shaderPackInUse = ModCompatibility.isShaderPackInUse();
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
        if (isGalaxyPickaxeMode(Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem())) {
            renderGalaxyPickaxePreview(event);
            event.setCanceled(true);
            return;
        }
        if (isEndgameSkyboxBlock(state)) {
            event.setCanceled(true);
        }
    }

    private static boolean isGalaxyPickaxeMode(ItemStack stack) {
        return !stack.isEmpty() && Xavitia.isGalaxyTool(stack) && GalaxyToolItem.isPickaxeMode(stack);
    }

    private static void renderGalaxyPickaxePreview(RenderHighlightEvent.Block event) {
        BlockPos center = Xavitia.galaxyPickaxeMiningCenter(event.getTarget().getBlockPos(), event.getTarget().getDirection());
        Direction face = event.getTarget().getDirection();
        Vec3 camera = event.getCamera().getPosition();
        double minX = center.getX() - 3 - camera.x;
        double minY = center.getY() - 3 - camera.y;
        double minZ = center.getZ() - 3 - camera.z;
        double maxX = center.getX() + 4 - camera.x;
        double maxY = center.getY() + 4 - camera.y;
        double maxZ = center.getZ() + 4 - camera.z;
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        PoseStack.Pose pose = event.getPoseStack().last();
        renderFaceGrid(pose, consumer, face, event.getTarget().getBlockPos(), camera, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void renderFaceGrid(PoseStack.Pose pose, VertexConsumer consumer, Direction face, BlockPos hoveredPos, Vec3 camera, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int red = 255;
        int green = 255;
        int blue = 255;
        int alpha = 255;
        switch (face) {
            case NORTH, SOUTH -> {
                double z = (face == Direction.NORTH ? hoveredPos.getZ() : hoveredPos.getZ() + 1.0D) - camera.z;
                for (int i = 0; i <= 7; i++) {
                    double x = minX + i;
                    addLine(pose, consumer, x, minY, z, x, maxY, z, red, green, blue, alpha);
                    double y = minY + i;
                    addLine(pose, consumer, minX, y, z, maxX, y, z, red, green, blue, alpha);
                }
            }
            case WEST, EAST -> {
                double x = (face == Direction.WEST ? hoveredPos.getX() : hoveredPos.getX() + 1.0D) - camera.x;
                for (int i = 0; i <= 7; i++) {
                    double z = minZ + i;
                    addLine(pose, consumer, x, minY, z, x, maxY, z, red, green, blue, alpha);
                    double y = minY + i;
                    addLine(pose, consumer, x, y, minZ, x, y, maxZ, red, green, blue, alpha);
                }
            }
            case DOWN, UP -> {
                double y = (face == Direction.DOWN ? hoveredPos.getY() : hoveredPos.getY() + 1.0D) - camera.y;
                for (int i = 0; i <= 7; i++) {
                    double x = minX + i;
                    addLine(pose, consumer, x, y, minZ, x, y, maxZ, red, green, blue, alpha);
                    double z = minZ + i;
                    addLine(pose, consumer, minX, y, z, maxX, y, z, red, green, blue, alpha);
                }
            }
        }
    }

    private static void addLine(PoseStack.Pose pose, VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, int red, int green, int blue, int alpha) {
        float nx = (float)(x2 - x1);
        float ny = (float)(y2 - y1);
        float nz = (float)(z2 - z1);
        float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0F) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        consumer.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
    }

    private static boolean isEndgameSkyboxBlock(BlockState state) {
        return state.is(Xavitia.GALAXY_COMPRESSOR_BLOCK.get())
                || state.is(Xavitia.GALAXY_CONNECTOR_BLOCK.get())
                || state.is(Xavitia.GALAXY_OBSIDIAN_BLOCK.get())
                || state.is(Xavitia.GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.BLUE_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.GREEN_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.RED_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.RAINBOW_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.YELLOW_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.GALAXY_BLOCK.get())
                || state.is(Xavitia.BLUE_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.GREEN_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.RED_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.RAINBOW_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.YELLOW_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.GALAXY_FREEZER_BLOCK.get());
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Xavitia.GALAXY_COMPRESSOR_MENU.get(), GalaxyCompressorScreen::new);
        event.register(Xavitia.GALAXY_FREEZER_MENU.get(), GalaxyFreezerScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Xavitia.GALAXY_COMPRESSOR_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(Xavitia.GALAXY_CONNECTOR_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(Xavitia.GALAXY_DECORATIVE_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(Xavitia.GALAXY_FREEZER_BLOCK_ENTITY.get(), EndgamePortalBlockEntityRenderer::new);
    }

    @SubscribeEvent
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WEATHER_CONTROLLER_HAND_MODEL);
        for (ModelResourceLocation location : GalaxyMultitoolItemRenderer.modelLocations()) {
            event.register(location);
        }
    }

    @SubscribeEvent
    static void wrapGalaxyIngotModel(ModelEvent.ModifyBakingResult event) {
        wrapGeneratedStencilItemModel(event, "galaxy_ingot");
        wrapGeneratedStencilItemModel(event, "galaxy_multitool");
        wrapGeneratedStencilItemModel(event, "day_night_toggle");
        wrapGeneratedStencilItemModel(event, "mob_annihilator");
        wrapGeneratedStencilItemModel(event, "reality_restorer");
        wrapGeneratedStencilItemModel(event, "survival_flight_core");
        wrapGeneratedStencilItemModel(event, "spectator_phase_core");
        wrapGeneratedStencilItemModel(event, "chunk_destroyer");
        wrapGeneratedStencilItemModel(event, "the_stick");

        ModelResourceLocation weatherLocation = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "weather_cycler"));
        BakedModel weatherOriginal = event.getModels().get(weatherLocation);
        BakedModel weatherController = event.getModels().get(WEATHER_CONTROLLER_HAND_MODEL);
        if (weatherOriginal != null && !(weatherOriginal instanceof WeatherControllerModel)) {
            event.getModels().put(weatherLocation, new WeatherControllerModel(weatherOriginal, weatherController));
        }

        wrapGalaxyToolModel(event, "galaxy_pickaxe", 0);
        wrapGalaxyToolModel(event, "galaxy_axe", 1);
        wrapGalaxyToolModel(event, "galaxy_hoe", 2);
        wrapGalaxyToolModel(event, "galaxy_shovel", 3);
        wrapGalaxyToolModel(event, "galaxy_sword", 4);
    }

    private static void wrapGeneratedStencilItemModel(ModelEvent.ModifyBakingResult event, String path) {
        ModelResourceLocation location = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, path));
        BakedModel original = event.getModels().get(location);
        if (original != null && !(original instanceof GeneratedStencilItemModel)) {
            event.getModels().put(location, new GeneratedStencilItemModel(original, path));
        }
    }

    private static void wrapGalaxyToolModel(ModelEvent.ModifyBakingResult event, String path, int toolIndex) {
        ModelResourceLocation location = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, path));
        BakedModel original = event.getModels().get(location);
        if (original != null && !(original instanceof GalaxyToolModel)) {
            event.getModels().put(location, new GalaxyToolModel(original, toolIndex));
        }
    }
}
