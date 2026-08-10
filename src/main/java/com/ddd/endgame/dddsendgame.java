package com.ddd.endgame;

import com.ddd.endgame.block.EndgameConnectorBlock;
import com.ddd.endgame.block.EndgameConnectorBlockEntity;
import com.ddd.endgame.block.EndgameControllerBlock;
import com.ddd.endgame.block.EndgameControllerBlockEntity;
import com.ddd.endgame.block.EndgameDecorativeBlock;
import com.ddd.endgame.block.EndgameDecorativeBlockEntity;
import com.ddd.endgame.block.EndgameSkyboxBlockItem;
import com.ddd.endgame.block.GalaxyBlockItem;
import com.ddd.endgame.block.GalaxyFreezerBlock;
import com.ddd.endgame.block.GalaxyFreezerBlockEntity;
import com.ddd.endgame.item.ChunkAnnihilatorItem;
import com.ddd.endgame.item.DayNightToggleItem;
import com.ddd.endgame.item.EndgameTestStickItem;
import com.ddd.endgame.item.EntityPurgeItem;
import com.ddd.endgame.item.GalaxyIngotItem;
import com.ddd.endgame.item.RandomBlockPlacerItem;
import com.ddd.endgame.item.RealityRestorerItem;
import com.ddd.endgame.item.SpectatorPhaseItem;
import com.ddd.endgame.item.SurvivalFlightItem;
import com.ddd.endgame.item.WeatherCycleItem;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod(dddsendgame.MODID)
public class dddsendgame {
    public static final String MODID = "dddsendgame";
    public static final long ENDGAME_ITEM_REQUIREMENT = 1_048_576L;
    private static final String ENDGAME_STICK_CREATIVE_KEY = MODID + ".endgame_stick_creative";
    private static final String SURVIVAL_FLIGHT_GRANTED_KEY = MODID + ".survival_flight_granted";
    private static final int SPECTATOR_PHASE_TICKS = 15 * 20;
    public static final int GALAXY_INSTABILITY_DETONATION_TICKS = 10 * 20;
    public static final float GALAXY_INSTABILITY_EXPLOSION_RADIUS = 8.0F;
    private static final Map<UUID, SpectatorPhaseState> SPECTATOR_PHASES = new HashMap<>();
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<Item> GALAXY_INGOT = ITEMS.register(
            "galaxy_ingot",
            () -> new GalaxyIngotItem(new Item.Properties())
    );
    public static final DeferredItem<Item> ENDGAME_TEST_STICK = ITEMS.register(
            "endgame_test_stick",
            () -> new EndgameTestStickItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> WEATHER_CYCLER = ITEMS.register(
            "weather_cycler",
            () -> new WeatherCycleItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> DAY_NIGHT_TOGGLE = ITEMS.register(
            "day_night_toggle",
            () -> new DayNightToggleItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> ENTITY_PURGE_CORE = ITEMS.register(
            "entity_purge_core",
            () -> new EntityPurgeItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> RANDOM_BLOCK_PLACER = ITEMS.register(
            "random_block_placer",
            () -> new RandomBlockPlacerItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> REALITY_RESTORER = ITEMS.register(
            "reality_restorer",
            () -> new RealityRestorerItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> SURVIVAL_FLIGHT_CORE = ITEMS.register(
            "survival_flight_core",
            () -> new SurvivalFlightItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> SPECTATOR_PHASE_CORE = ITEMS.register(
            "spectator_phase_core",
            () -> new SpectatorPhaseItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> CHUNK_ANNIHILATOR = ITEMS.register(
            "chunk_annihilator",
            () -> new ChunkAnnihilatorItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredBlock<EndgameControllerBlock> ENDGAME_CONTROLLER_BLOCK = BLOCKS.registerBlock(
            "endgame_controller",
            EndgameControllerBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
    );
    public static final DeferredItem<BlockItem> ENDGAME_CONTROLLER_ITEM = ITEMS.register(
            "endgame_controller",
            () -> new EndgameSkyboxBlockItem(ENDGAME_CONTROLLER_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<EndgameConnectorBlock> ENDGAME_CONNECTOR_BLOCK = BLOCKS.registerBlock(
            "endgame_connector",
            EndgameConnectorBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).sound(SoundType.STONE)
    );
    public static final DeferredItem<BlockItem> ENDGAME_CONNECTOR_ITEM = ITEMS.register(
            "endgame_connector",
            () -> new EndgameSkyboxBlockItem(ENDGAME_CONNECTOR_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<EndgameDecorativeBlock> ENDGAME_SOLID_BLOCK = BLOCKS.registerBlock(
            "endgame_solid_block",
            EndgameDecorativeBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).sound(SoundType.STONE)
    );
    public static final DeferredItem<BlockItem> ENDGAME_SOLID_BLOCK_ITEM = ITEMS.register(
            "endgame_solid_block",
            () -> new EndgameSkyboxBlockItem(ENDGAME_SOLID_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<EndgameDecorativeBlock> ENDGAME_GLASS_BLOCK = BLOCKS.registerBlock(
            "endgame_glass",
            EndgameDecorativeBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).sound(SoundType.STONE)
    );
    public static final DeferredItem<BlockItem> ENDGAME_GLASS_ITEM = ITEMS.register(
            "endgame_glass",
            () -> new EndgameSkyboxBlockItem(ENDGAME_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<TransparentBlock> ENDGAME_EMPTY_GLASS_BLOCK = registerTransparentGlassBlock("endgame_empty_glass", 5.4F);
    public static final DeferredItem<BlockItem> ENDGAME_EMPTY_GLASS_ITEM = registerBlockItem("endgame_empty_glass", ENDGAME_EMPTY_GLASS_BLOCK);
    public static final DeferredBlock<EndgameDecorativeBlock> ENDGAME_FULL_GLASS_BLOCK = BLOCKS.registerBlock(
            "endgame_full_glass",
            EndgameDecorativeBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).sound(SoundType.STONE)
    );
    public static final DeferredItem<BlockItem> ENDGAME_FULL_GLASS_ITEM = ITEMS.register(
            "endgame_full_glass",
            () -> new EndgameSkyboxBlockItem(ENDGAME_FULL_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<EndgameDecorativeBlock> GALAXY_BLOCK = BLOCKS.registerBlock(
            "galaxy_block",
            EndgameDecorativeBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).sound(SoundType.STONE)
    );
    public static final DeferredItem<BlockItem> GALAXY_BLOCK_ITEM = ITEMS.register(
            "galaxy_block",
            () -> new GalaxyBlockItem(GALAXY_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyFreezerBlock> GALAXY_FREEZER_BLOCK = BLOCKS.registerBlock(
            "galaxy_freezer",
            GalaxyFreezerBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
    );
    public static final DeferredItem<BlockItem> GALAXY_FREEZER_ITEM = ITEMS.register(
            "galaxy_freezer",
            () -> new EndgameSkyboxBlockItem(GALAXY_FREEZER_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_1_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_1");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_1_ITEM = registerBlockItem("compressed_obsidian_1", COMPRESSED_OBSIDIAN_1_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_2_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_2");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_2_ITEM = registerBlockItem("compressed_obsidian_2", COMPRESSED_OBSIDIAN_2_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_3_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_3");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_3_ITEM = registerBlockItem("compressed_obsidian_3", COMPRESSED_OBSIDIAN_3_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_4_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_4");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_4_ITEM = registerBlockItem("compressed_obsidian_4", COMPRESSED_OBSIDIAN_4_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_5_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_5");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_5_ITEM = registerBlockItem("compressed_obsidian_5", COMPRESSED_OBSIDIAN_5_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_6_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_6");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_6_ITEM = registerBlockItem("compressed_obsidian_6", COMPRESSED_OBSIDIAN_6_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_7_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_7");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_7_ITEM = registerBlockItem("compressed_obsidian_7", COMPRESSED_OBSIDIAN_7_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_OBSIDIAN_8_BLOCK = registerCompressedObsidianBlock("compressed_obsidian_8");
    public static final DeferredItem<BlockItem> COMPRESSED_OBSIDIAN_8_ITEM = registerBlockItem("compressed_obsidian_8", COMPRESSED_OBSIDIAN_8_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_1_BLOCK = registerCompressedGlassBlock("compressed_glass_1", 0.6F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_1_ITEM = registerBlockItem("compressed_glass_1", COMPRESSED_GLASS_1_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_2_BLOCK = registerCompressedGlassBlock("compressed_glass_2", 1.2F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_2_ITEM = registerBlockItem("compressed_glass_2", COMPRESSED_GLASS_2_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_3_BLOCK = registerCompressedGlassBlock("compressed_glass_3", 1.8F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_3_ITEM = registerBlockItem("compressed_glass_3", COMPRESSED_GLASS_3_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_4_BLOCK = registerCompressedGlassBlock("compressed_glass_4", 2.4F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_4_ITEM = registerBlockItem("compressed_glass_4", COMPRESSED_GLASS_4_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_5_BLOCK = registerCompressedGlassBlock("compressed_glass_5", 3.0F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_5_ITEM = registerBlockItem("compressed_glass_5", COMPRESSED_GLASS_5_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_6_BLOCK = registerCompressedGlassBlock("compressed_glass_6", 3.6F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_6_ITEM = registerBlockItem("compressed_glass_6", COMPRESSED_GLASS_6_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_7_BLOCK = registerCompressedGlassBlock("compressed_glass_7", 4.2F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_7_ITEM = registerBlockItem("compressed_glass_7", COMPRESSED_GLASS_7_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_8_BLOCK = registerCompressedGlassBlock("compressed_glass_8", 4.8F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_8_ITEM = registerBlockItem("compressed_glass_8", COMPRESSED_GLASS_8_BLOCK);
    public static final DeferredBlock<TransparentBlock> COMPRESSED_GLASS_9_BLOCK = registerCompressedGlassBlock("compressed_glass_9", 5.4F);
    public static final DeferredItem<BlockItem> COMPRESSED_GLASS_9_ITEM = registerBlockItem("compressed_glass_9", COMPRESSED_GLASS_9_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndgameControllerBlockEntity>> ENDGAME_CONTROLLER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("endgame_controller", () -> BlockEntityType.Builder.of(EndgameControllerBlockEntity::new, ENDGAME_CONTROLLER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndgameConnectorBlockEntity>> ENDGAME_CONNECTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("endgame_connector", () -> BlockEntityType.Builder.of(EndgameConnectorBlockEntity::new, ENDGAME_CONNECTOR_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndgameDecorativeBlockEntity>> ENDGAME_DECORATIVE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("endgame_decorative", () -> BlockEntityType.Builder.of(
                    EndgameDecorativeBlockEntity::new,
                    ENDGAME_SOLID_BLOCK.get(),
                    ENDGAME_GLASS_BLOCK.get(),
                    ENDGAME_FULL_GLASS_BLOCK.get(),
                    GALAXY_BLOCK.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GalaxyFreezerBlockEntity>> GALAXY_FREEZER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("galaxy_freezer", () -> BlockEntityType.Builder.of(GalaxyFreezerBlockEntity::new, GALAXY_FREEZER_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<EndgameControllerMenu>> ENDGAME_CONTROLLER_MENU =
            MENU_TYPES.register("endgame_controller", () -> IMenuTypeExtension.create(EndgameControllerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GalaxyFreezerMenu>> GALAXY_FREEZER_MENU =
            MENU_TYPES.register("galaxy_freezer", () -> IMenuTypeExtension.create(GalaxyFreezerMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENDGAME_TAB = CREATIVE_MODE_TABS.register("endgame_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dddsendgame"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ENDGAME_CONTROLLER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ENDGAME_CONTROLLER_ITEM.get());
                output.accept(ENDGAME_CONNECTOR_ITEM.get());
                output.accept(ENDGAME_SOLID_BLOCK_ITEM.get());
                output.accept(ENDGAME_GLASS_ITEM.get());
                output.accept(ENDGAME_EMPTY_GLASS_ITEM.get());
                output.accept(ENDGAME_FULL_GLASS_ITEM.get());
                output.accept(GALAXY_BLOCK_ITEM.get());
                output.accept(GALAXY_FREEZER_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_1_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_2_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_3_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_4_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_5_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_6_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_7_ITEM.get());
                output.accept(COMPRESSED_OBSIDIAN_8_ITEM.get());
                output.accept(COMPRESSED_GLASS_1_ITEM.get());
                output.accept(COMPRESSED_GLASS_2_ITEM.get());
                output.accept(COMPRESSED_GLASS_3_ITEM.get());
                output.accept(COMPRESSED_GLASS_4_ITEM.get());
                output.accept(COMPRESSED_GLASS_5_ITEM.get());
                output.accept(COMPRESSED_GLASS_6_ITEM.get());
                output.accept(COMPRESSED_GLASS_7_ITEM.get());
                output.accept(COMPRESSED_GLASS_8_ITEM.get());
                output.accept(COMPRESSED_GLASS_9_ITEM.get());
                output.accept(WEATHER_CYCLER.get());
                output.accept(DAY_NIGHT_TOGGLE.get());
                output.accept(ENTITY_PURGE_CORE.get());
                output.accept(RANDOM_BLOCK_PLACER.get());
                output.accept(REALITY_RESTORER.get());
                output.accept(SURVIVAL_FLIGHT_CORE.get());
                output.accept(SPECTATOR_PHASE_CORE.get());
                output.accept(CHUNK_ANNIHILATOR.get());
                output.accept(GALAXY_INGOT.get());
                output.accept(GALAXY_BLOCK_ITEM.get());
                output.accept(EndgameTestRecipe.createResult(parameters.holders()));
            }).build());

    public dddsendgame(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ENDGAME_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ENDGAME_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ENDGAME_CONNECTOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ENDGAME_CONNECTOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GALAXY_FREEZER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("ddd's endgame common setup complete");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ENDGAME_CONTROLLER_ITEM);
            event.accept(ENDGAME_CONNECTOR_ITEM);
            event.accept(ENDGAME_SOLID_BLOCK_ITEM);
            event.accept(ENDGAME_GLASS_ITEM);
            event.accept(ENDGAME_EMPTY_GLASS_ITEM);
            event.accept(ENDGAME_FULL_GLASS_ITEM);
            event.accept(GALAXY_BLOCK_ITEM);
            event.accept(GALAXY_FREEZER_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_1_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_2_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_3_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_4_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_5_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_6_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_7_ITEM);
            event.accept(COMPRESSED_OBSIDIAN_8_ITEM);
            event.accept(COMPRESSED_GLASS_1_ITEM);
            event.accept(COMPRESSED_GLASS_2_ITEM);
            event.accept(COMPRESSED_GLASS_3_ITEM);
            event.accept(COMPRESSED_GLASS_4_ITEM);
            event.accept(COMPRESSED_GLASS_5_ITEM);
            event.accept(COMPRESSED_GLASS_6_ITEM);
            event.accept(COMPRESSED_GLASS_7_ITEM);
            event.accept(COMPRESSED_GLASS_8_ITEM);
            event.accept(COMPRESSED_GLASS_9_ITEM);
        }
    }

    private static DeferredBlock<Block> registerCompressedObsidianBlock(String name) {
        return BLOCKS.registerBlock(name, Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN));
    }

    private static DeferredBlock<TransparentBlock> registerCompressedGlassBlock(String name, float destroyTime) {
        return registerTransparentGlassBlock(name, destroyTime);
    }

    private static DeferredBlock<TransparentBlock> registerTransparentGlassBlock(String name, float destroyTime) {
        return BLOCKS.registerBlock(name, TransparentBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(destroyTime, 6.0F));
    }

    private static DeferredItem<BlockItem> registerBlockItem(String name, DeferredBlock<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        updateSurvivalFlight(player);
        GalaxyInstability.tickPlayerStacks(player);
        boolean changedByStick = player.getPersistentData().getBoolean(ENDGAME_STICK_CREATIVE_KEY);
        if (!Config.ENDGAME_STICK_GRANTS_CREATIVE.getAsBoolean()) {
            if (changedByStick) {
                if (player.isCreative()) {
                    player.setGameMode(GameType.SURVIVAL);
                }
                player.getPersistentData().remove(ENDGAME_STICK_CREATIVE_KEY);
            }
            return;
        }

        boolean hasEndgameStick = player.getInventory().contains(stack -> stack.is(ENDGAME_TEST_STICK.get()));
        if (hasEndgameStick) {
            if (!player.isCreative()) {
                if (player.setGameMode(GameType.CREATIVE)) {
                    player.getPersistentData().putBoolean(ENDGAME_STICK_CREATIVE_KEY, true);
                }
            }
            return;
        }

        if (changedByStick) {
            if (player.isCreative()) {
                player.setGameMode(GameType.SURVIVAL);
            }
            player.getPersistentData().remove(ENDGAME_STICK_CREATIVE_KEY);
        }
    }

    private static void updateSurvivalFlight(ServerPlayer player) {
        boolean grantedByItem = player.getPersistentData().getBoolean(SURVIVAL_FLIGHT_GRANTED_KEY);
        boolean hasFlightCore = player.getInventory().contains(stack -> stack.is(SURVIVAL_FLIGHT_CORE.get()));

        if (hasFlightCore) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            player.getPersistentData().putBoolean(SURVIVAL_FLIGHT_GRANTED_KEY, true);
            return;
        }

        if (!grantedByItem) {
            return;
        }

        player.getPersistentData().remove(SURVIVAL_FLIGHT_GRANTED_KEY);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickSpectatorPhases(event.getServer());
    }

    public static void startSpectatorPhase(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (SPECTATOR_PHASES.containsKey(playerId)) {
            return;
        }

        SpectatorPhaseState state = new SpectatorPhaseState(
                player.serverLevel().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.gameMode.getGameModeForPlayer(),
                SPECTATOR_PHASE_TICKS
        );
        SPECTATOR_PHASES.put(playerId, state);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SPECTATOR_PHASE_TICKS, 0, false, false, false));
        player.setGameMode(GameType.SPECTATOR);
        player.displayClientMessage(Component.translatable("message.dddsendgame.spectator_phase.enter"), true);
    }

    private static void tickSpectatorPhases(MinecraftServer server) {
        if (server == null || SPECTATOR_PHASES.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, SpectatorPhaseState>> iterator = SPECTATOR_PHASES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SpectatorPhaseState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            SpectatorPhaseState state = entry.getValue().tick();
            if (state.remainingTicks() > 0) {
                entry.setValue(state);
                int secondsRemaining = (state.remainingTicks() + 19) / 20;
                player.displayClientMessage(Component.translatable("message.dddsendgame.spectator_phase.countdown", secondsRemaining), true);
                continue;
            }

            ServerLevel originalLevel = server.getLevel(state.dimension());
            ServerLevel targetLevel = originalLevel != null ? originalLevel : player.serverLevel();
            player.teleportTo(targetLevel, state.x(), state.y(), state.z(), state.yRot(), state.xRot());
            player.setGameMode(state.gameType());
            player.displayClientMessage(Component.translatable("message.dddsendgame.spectator_phase.return"), true);
            iterator.remove();
        }
    }

    private record SpectatorPhaseState(
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            GameType gameType,
            int remainingTicks
    ) {
        private SpectatorPhaseState tick() {
            return new SpectatorPhaseState(dimension, x, y, z, yRot, xRot, gameType, remainingTicks - 1);
        }
    }
}
