package com.ddd.endgame;

import com.ddd.endgame.galaxy.GalaxyInstability;

import com.ddd.endgame.galaxy.GalaxyFreezerMenu;

import com.ddd.endgame.galaxy.GalaxyCompressorMenu;

import com.ddd.endgame.block.GalaxyConnectorBlock;
import com.ddd.endgame.block.GalaxyConnectorBlockEntity;
import com.ddd.endgame.block.GalaxyCompressorBlock;
import com.ddd.endgame.block.GalaxyCompressorBlockEntity;
import com.ddd.endgame.block.DescribedEndgameSkyboxBlockItem;
import com.ddd.endgame.block.GalaxyDecorativeBlock;
import com.ddd.endgame.block.GalaxyDecorativeBlockEntity;
import com.ddd.endgame.block.EndgameSkyboxBlockItem;
import com.ddd.endgame.block.GalaxyBlockItem;
import com.ddd.endgame.block.GalaxyFreezerBlock;
import com.ddd.endgame.block.GalaxyFreezerBlockEntity;
import com.ddd.endgame.compat.BlockFabricatorInventoryHandler;
import com.ddd.endgame.item.ChunkAnnihilatorItem;
import com.ddd.endgame.item.DayNightToggleItem;
import com.ddd.endgame.item.TheStickItem;
import com.ddd.endgame.item.EntityPurgeItem;
import com.ddd.endgame.item.GalaxyMultitoolItem;
import com.ddd.endgame.item.GalaxyToolItem;
import com.ddd.endgame.item.GalaxyIngotItem;
import com.ddd.endgame.item.RandomBlockPlacerItem;
import com.ddd.endgame.item.RealityRestorerItem;
import com.ddd.endgame.item.SpectatorPhaseItem;
import com.ddd.endgame.item.SurvivalFlightItem;
import com.ddd.endgame.item.WeatherCycleItem;
import com.ddd.endgame.payload.BlockFabricatorSelectionPayload;
import com.ddd.endgame.payload.GalaxyMultitoolSelectionPayload;
import com.ddd.endgame.payload.TheStickModePayload;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.items.VanillaHopperItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@Mod(Xavitia.MODID)
public class Xavitia {
    public static final String MODID = "xavitia";
    public static final long ENDGAME_ITEM_REQUIREMENT = 1_048_576L;
    private static final String THE_STICK_CREATIVE_KEY = MODID + ".the_stick_creative";
    private static final String THE_STICK_COMMAND_TREE_KEY = MODID + ".the_stick_command_tree";
    private static final String SURVIVAL_FLIGHT_GRANTED_KEY = MODID + ".survival_flight_granted";
    private static final int SPECTATOR_PHASE_TICKS = 15 * 20;
    public static final int GALAXY_INSTABILITY_DETONATION_TICKS = 10 * 20;
    private static final int GALAXY_AXE_RADIUS = 5;
    private static final int GALAXY_AXE_MAX_BRANCH_BLOCKS = 128;
    private static final int GALAXY_SHOVEL_HORIZONTAL_RADIUS = 5;
    private static final int GALAXY_SHOVEL_VERTICAL_RADIUS = 3;
    private static final Map<UUID, SpectatorPhaseState> SPECTATOR_PHASES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> FABRICATOR_HOPPER_COOLDOWNS = new HashMap<>();
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
    public static final DeferredItem<Item> THE_STICK = ITEMS.register(
            "the_stick",
            () -> new TheStickItem(new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.DEBUG_STICK_STATE, DebugStickState.EMPTY))
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
    public static final DeferredItem<Item> GALAXY_MULTITOOL = ITEMS.register(
            "galaxy_multitool",
            () -> new GalaxyMultitoolItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> GALAXY_PICKAXE = ITEMS.register(
            "galaxy_pickaxe",
            () -> new GalaxyToolItem(0, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> GALAXY_AXE = ITEMS.register(
            "galaxy_axe",
            () -> new GalaxyToolItem(1, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> GALAXY_HOE = ITEMS.register(
            "galaxy_hoe",
            () -> new GalaxyToolItem(2, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> GALAXY_SHOVEL = ITEMS.register(
            "galaxy_shovel",
            () -> new GalaxyToolItem(3, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> GALAXY_SWORD = ITEMS.register(
            "galaxy_sword",
            () -> new GalaxyToolItem(4, new Item.Properties().stacksTo(1))
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
    public static final DeferredBlock<GalaxyCompressorBlock> GALAXY_COMPRESSOR_BLOCK = BLOCKS.registerBlock(
            "galaxy_compressor",
            GalaxyCompressorBlock::new,
            galaxyBlockProperties(Blocks.OBSIDIAN)
    );
    public static final DeferredItem<BlockItem> GALAXY_COMPRESSOR_ITEM = ITEMS.register(
            "galaxy_compressor",
            () -> new DescribedEndgameSkyboxBlockItem(GALAXY_COMPRESSOR_BLOCK.get(), new Item.Properties(), "block.xavitia.galaxy_compressor.tooltip")
    );
    public static final DeferredBlock<GalaxyConnectorBlock> GALAXY_CONNECTOR_BLOCK = BLOCKS.registerBlock(
            "galaxy_connector",
            GalaxyConnectorBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> GALAXY_CONNECTOR_ITEM = ITEMS.register(
            "galaxy_connector",
            () -> new DescribedEndgameSkyboxBlockItem(GALAXY_CONNECTOR_BLOCK.get(), new Item.Properties(), "block.xavitia.galaxy_connector.tooltip")
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> GALAXY_OBSIDIAN_BLOCK = BLOCKS.registerBlock(
            "galaxy_obsidian",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> GALAXY_OBSIDIAN_BLOCK_ITEM = ITEMS.register(
            "galaxy_obsidian",
            () -> new EndgameSkyboxBlockItem(GALAXY_OBSIDIAN_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> GALAXY_GLASS_BLOCK = BLOCKS.registerBlock(
            "galaxy_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> GALAXY_GLASS_ITEM = ITEMS.register(
            "galaxy_glass",
            () -> new EndgameSkyboxBlockItem(GALAXY_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> BLUE_GALAXY_GLASS_BLOCK = BLOCKS.registerBlock(
            "blue_galaxy_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> BLUE_GALAXY_GLASS_ITEM = ITEMS.register(
            "blue_galaxy_glass",
            () -> new EndgameSkyboxBlockItem(BLUE_GALAXY_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> GREEN_GALAXY_GLASS_BLOCK = BLOCKS.registerBlock(
            "green_galaxy_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> GREEN_GALAXY_GLASS_ITEM = ITEMS.register(
            "green_galaxy_glass",
            () -> new EndgameSkyboxBlockItem(GREEN_GALAXY_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> RED_GALAXY_GLASS_BLOCK = BLOCKS.registerBlock(
            "red_galaxy_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> RED_GALAXY_GLASS_ITEM = ITEMS.register(
            "red_galaxy_glass",
            () -> new EndgameSkyboxBlockItem(RED_GALAXY_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> RAINBOW_GALAXY_GLASS_BLOCK = BLOCKS.registerBlock(
            "rainbow_galaxy_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> RAINBOW_GALAXY_GLASS_ITEM = ITEMS.register(
            "rainbow_galaxy_glass",
            () -> new EndgameSkyboxBlockItem(RAINBOW_GALAXY_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> YELLOW_GALAXY_GLASS_BLOCK = BLOCKS.registerBlock(
            "yellow_galaxy_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> YELLOW_GALAXY_GLASS_ITEM = ITEMS.register(
            "yellow_galaxy_glass",
            () -> new EndgameSkyboxBlockItem(YELLOW_GALAXY_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<TransparentBlock> GALAXY_HARDENED_GLASS_BLOCK = registerTransparentGlassBlock("galaxy_hardened_glass", 5.4F);
    public static final DeferredItem<BlockItem> GALAXY_HARDENED_GLASS_ITEM = registerBlockItem("galaxy_hardened_glass", GALAXY_HARDENED_GLASS_BLOCK);
    public static final DeferredBlock<GalaxyDecorativeBlock> GALAXY_FULL_GLASS_BLOCK = BLOCKS.registerBlock(
            "galaxy_full_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> GALAXY_FULL_GLASS_ITEM = ITEMS.register(
            "galaxy_full_glass",
            () -> new EndgameSkyboxBlockItem(GALAXY_FULL_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> GALAXY_BLOCK = BLOCKS.registerBlock(
            "galaxy_block",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> GALAXY_BLOCK_ITEM = ITEMS.register(
            "galaxy_block",
            () -> new GalaxyBlockItem(GALAXY_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> BLUE_GALAXY_FULL_GLASS_BLOCK = BLOCKS.registerBlock(
            "blue_galaxy_full_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> BLUE_GALAXY_FULL_GLASS_BLOCK_ITEM = ITEMS.register(
            "blue_galaxy_full_glass",
            () -> new EndgameSkyboxBlockItem(BLUE_GALAXY_FULL_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> GREEN_GALAXY_FULL_GLASS_BLOCK = BLOCKS.registerBlock(
            "green_galaxy_full_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> GREEN_GALAXY_FULL_GLASS_BLOCK_ITEM = ITEMS.register(
            "green_galaxy_full_glass",
            () -> new EndgameSkyboxBlockItem(GREEN_GALAXY_FULL_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> RED_GALAXY_FULL_GLASS_BLOCK = BLOCKS.registerBlock(
            "red_galaxy_full_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> RED_GALAXY_FULL_GLASS_BLOCK_ITEM = ITEMS.register(
            "red_galaxy_full_glass",
            () -> new EndgameSkyboxBlockItem(RED_GALAXY_FULL_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> RAINBOW_GALAXY_FULL_GLASS_BLOCK = BLOCKS.registerBlock(
            "rainbow_galaxy_full_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> RAINBOW_GALAXY_FULL_GLASS_BLOCK_ITEM = ITEMS.register(
            "rainbow_galaxy_full_glass",
            () -> new EndgameSkyboxBlockItem(RAINBOW_GALAXY_FULL_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyDecorativeBlock> YELLOW_GALAXY_FULL_GLASS_BLOCK = BLOCKS.registerBlock(
            "yellow_galaxy_full_glass",
            GalaxyDecorativeBlock::new,
            galaxyBlockProperties(Blocks.DEEPSLATE)
    );
    public static final DeferredItem<BlockItem> YELLOW_GALAXY_FULL_GLASS_BLOCK_ITEM = ITEMS.register(
            "yellow_galaxy_full_glass",
            () -> new EndgameSkyboxBlockItem(YELLOW_GALAXY_FULL_GLASS_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<GalaxyFreezerBlock> GALAXY_FREEZER_BLOCK = BLOCKS.registerBlock(
            "galaxy_freezer",
            GalaxyFreezerBlock::new,
            galaxyBlockProperties(Blocks.OBSIDIAN)
    );
    public static final DeferredItem<BlockItem> GALAXY_FREEZER_ITEM = ITEMS.register(
            "galaxy_freezer",
            () -> new DescribedEndgameSkyboxBlockItem(GALAXY_FREEZER_BLOCK.get(), new Item.Properties(), "block.xavitia.galaxy_freezer.tooltip")
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
    public static final DeferredBlock<Block> COMPRESSED_ICE_1_BLOCK = registerCompressedIceBlock("compressed_ice_1", 1.2F);
    public static final DeferredItem<BlockItem> COMPRESSED_ICE_1_ITEM = registerBlockItem("compressed_ice_1", COMPRESSED_ICE_1_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_ICE_2_BLOCK = registerCompressedIceBlock("compressed_ice_2", 2.4F);
    public static final DeferredItem<BlockItem> COMPRESSED_ICE_2_ITEM = registerBlockItem("compressed_ice_2", COMPRESSED_ICE_2_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_ICE_3_BLOCK = registerCompressedIceBlock("compressed_ice_3", 3.6F);
    public static final DeferredItem<BlockItem> COMPRESSED_ICE_3_ITEM = registerBlockItem("compressed_ice_3", COMPRESSED_ICE_3_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_ICE_4_BLOCK = registerCompressedIceBlock("compressed_ice_4", 4.8F);
    public static final DeferredItem<BlockItem> COMPRESSED_ICE_4_ITEM = registerBlockItem("compressed_ice_4", COMPRESSED_ICE_4_BLOCK);
    public static final DeferredBlock<Block> COMPRESSED_ICE_5_BLOCK = registerCompressedIceBlock("compressed_ice_5", 6.0F);
    public static final DeferredItem<BlockItem> COMPRESSED_ICE_5_ITEM = registerBlockItem("compressed_ice_5", COMPRESSED_ICE_5_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GalaxyCompressorBlockEntity>> GALAXY_COMPRESSOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("galaxy_compressor", () -> BlockEntityType.Builder.of(GalaxyCompressorBlockEntity::new, GALAXY_COMPRESSOR_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GalaxyConnectorBlockEntity>> GALAXY_CONNECTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("galaxy_connector", () -> BlockEntityType.Builder.of(GalaxyConnectorBlockEntity::new, GALAXY_CONNECTOR_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GalaxyDecorativeBlockEntity>> GALAXY_DECORATIVE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("galaxy_decorative", () -> BlockEntityType.Builder.of(
                    GalaxyDecorativeBlockEntity::new,
                    GALAXY_OBSIDIAN_BLOCK.get(),
                    GALAXY_GLASS_BLOCK.get(),
                    BLUE_GALAXY_GLASS_BLOCK.get(),
                    GREEN_GALAXY_GLASS_BLOCK.get(),
                    RED_GALAXY_GLASS_BLOCK.get(),
                    RAINBOW_GALAXY_GLASS_BLOCK.get(),
                    YELLOW_GALAXY_GLASS_BLOCK.get(),
                    GALAXY_FULL_GLASS_BLOCK.get(),
                    GALAXY_BLOCK.get(),
                    BLUE_GALAXY_FULL_GLASS_BLOCK.get(),
                    GREEN_GALAXY_FULL_GLASS_BLOCK.get(),
                    RED_GALAXY_FULL_GLASS_BLOCK.get(),
                    RAINBOW_GALAXY_FULL_GLASS_BLOCK.get(),
                    YELLOW_GALAXY_FULL_GLASS_BLOCK.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GalaxyFreezerBlockEntity>> GALAXY_FREEZER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("galaxy_freezer", () -> BlockEntityType.Builder.of(GalaxyFreezerBlockEntity::new, GALAXY_FREEZER_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<GalaxyCompressorMenu>> GALAXY_COMPRESSOR_MENU =
            MENU_TYPES.register("galaxy_compressor", () -> IMenuTypeExtension.create(GalaxyCompressorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GalaxyFreezerMenu>> GALAXY_FREEZER_MENU =
            MENU_TYPES.register("galaxy_freezer", () -> IMenuTypeExtension.create(GalaxyFreezerMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENDGAME_TAB = CREATIVE_MODE_TABS.register("endgame_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xavitia"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> GALAXY_COMPRESSOR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(GALAXY_COMPRESSOR_ITEM.get());
                output.accept(GALAXY_CONNECTOR_ITEM.get());
                output.accept(GALAXY_OBSIDIAN_BLOCK_ITEM.get());
                output.accept(GALAXY_GLASS_ITEM.get());
                output.accept(BLUE_GALAXY_GLASS_ITEM.get());
                output.accept(GREEN_GALAXY_GLASS_ITEM.get());
                output.accept(RED_GALAXY_GLASS_ITEM.get());
                output.accept(RAINBOW_GALAXY_GLASS_ITEM.get());
                output.accept(YELLOW_GALAXY_GLASS_ITEM.get());
                output.accept(GALAXY_HARDENED_GLASS_ITEM.get());
                output.accept(GALAXY_FULL_GLASS_ITEM.get());
                output.accept(GALAXY_BLOCK_ITEM.get());
                output.accept(BLUE_GALAXY_FULL_GLASS_BLOCK_ITEM.get());
                output.accept(GREEN_GALAXY_FULL_GLASS_BLOCK_ITEM.get());
                output.accept(RED_GALAXY_FULL_GLASS_BLOCK_ITEM.get());
                output.accept(RAINBOW_GALAXY_FULL_GLASS_BLOCK_ITEM.get());
                output.accept(YELLOW_GALAXY_FULL_GLASS_BLOCK_ITEM.get());
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
                output.accept(COMPRESSED_ICE_1_ITEM.get());
                output.accept(COMPRESSED_ICE_2_ITEM.get());
                output.accept(COMPRESSED_ICE_3_ITEM.get());
                output.accept(COMPRESSED_ICE_4_ITEM.get());
                output.accept(COMPRESSED_ICE_5_ITEM.get());
                output.accept(WEATHER_CYCLER.get());
                output.accept(DAY_NIGHT_TOGGLE.get());
                output.accept(ENTITY_PURGE_CORE.get());
                output.accept(RANDOM_BLOCK_PLACER.get());
                output.accept(GALAXY_MULTITOOL.get());
                output.accept(GALAXY_PICKAXE.get());
                output.accept(GALAXY_AXE.get());
                output.accept(GALAXY_HOE.get());
                output.accept(GALAXY_SHOVEL.get());
                output.accept(GALAXY_SWORD.get());
                output.accept(REALITY_RESTORER.get());
                output.accept(SURVIVAL_FLIGHT_CORE.get());
                output.accept(SPECTATOR_PHASE_CORE.get());
                output.accept(CHUNK_ANNIHILATOR.get());
                output.accept(GALAXY_INGOT.get());
                output.accept(GALAXY_BLOCK_ITEM.get());
                output.accept(TheStickRecipe.createResult(parameters.holders()));
            }).build());

    public Xavitia(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(EventPriority.HIGH, this::registerCapabilities);
        modEventBus.addListener(this::registerPayloads);

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
        registerBlockFabricatorAutomationCapabilities(event);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GALAXY_COMPRESSOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                GALAXY_COMPRESSOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GALAXY_CONNECTOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                GALAXY_CONNECTOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GALAXY_FREEZER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.directAutomationHandler()
        );
        event.registerItem(
                Capabilities.ItemHandler.ITEM,
                (stack, context) -> RandomBlockPlacerItem.infiniteItemHandler(stack),
                RANDOM_BLOCK_PLACER.get()
        );
    }

    private static void registerBlockFabricatorAutomationCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> {
                    Container container = ChestBlock.getContainer((ChestBlock) state.getBlock(), state, level, pos, true);
                    return container == null ? null : new BlockFabricatorInventoryHandler(new InvWrapper(container));
                },
                Blocks.CHEST,
                Blocks.TRAPPED_CHEST
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityType.HOPPER,
                (hopper, side) -> new BlockFabricatorInventoryHandler(new VanillaHopperItemHandler(hopper))
        );

        registerFabricatorSidedContainer(event, BlockEntityType.BLAST_FURNACE);
        registerFabricatorSidedContainer(event, BlockEntityType.BREWING_STAND);
        registerFabricatorSidedContainer(event, BlockEntityType.FURNACE);
        registerFabricatorSidedContainer(event, BlockEntityType.SMOKER);
        registerFabricatorSidedContainer(event, BlockEntityType.SHULKER_BOX);

        registerFabricatorContainer(event, BlockEntityType.BARREL);
        registerFabricatorContainer(event, BlockEntityType.CHISELED_BOOKSHELF);
        registerFabricatorContainer(event, BlockEntityType.DISPENSER);
        registerFabricatorContainer(event, BlockEntityType.DROPPER);
        registerFabricatorContainer(event, BlockEntityType.JUKEBOX);
        registerFabricatorContainer(event, BlockEntityType.CRAFTER);
        registerFabricatorContainer(event, BlockEntityType.DECORATED_POT);
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity & Container> void registerFabricatorContainer(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (container, side) -> new BlockFabricatorInventoryHandler(new InvWrapper(container))
        );
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity & WorldlyContainer> void registerFabricatorSidedContainer(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (container, side) -> new BlockFabricatorInventoryHandler(new SidedInvWrapper(container, side))
        );
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                TheStickModePayload.TYPE,
                TheStickModePayload.STREAM_CODEC,
                TheStickModePayload::handle
        );
        event.registrar("1").playToServer(
                BlockFabricatorSelectionPayload.TYPE,
                BlockFabricatorSelectionPayload.STREAM_CODEC,
                BlockFabricatorSelectionPayload::handle
        );
        event.registrar("1").playToServer(
                GalaxyMultitoolSelectionPayload.TYPE,
                GalaxyMultitoolSelectionPayload.STREAM_CODEC,
                GalaxyMultitoolSelectionPayload::handle
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Xavitia common setup complete");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(GALAXY_COMPRESSOR_ITEM);
            event.accept(GALAXY_CONNECTOR_ITEM);
            event.accept(GALAXY_OBSIDIAN_BLOCK_ITEM);
            event.accept(GALAXY_GLASS_ITEM);
            event.accept(BLUE_GALAXY_GLASS_ITEM);
            event.accept(GREEN_GALAXY_GLASS_ITEM);
            event.accept(RED_GALAXY_GLASS_ITEM);
            event.accept(RAINBOW_GALAXY_GLASS_ITEM);
            event.accept(YELLOW_GALAXY_GLASS_ITEM);
            event.accept(GALAXY_HARDENED_GLASS_ITEM);
            event.accept(GALAXY_FULL_GLASS_ITEM);
            event.accept(GALAXY_BLOCK_ITEM);
            event.accept(BLUE_GALAXY_FULL_GLASS_BLOCK_ITEM);
            event.accept(GREEN_GALAXY_FULL_GLASS_BLOCK_ITEM);
            event.accept(RED_GALAXY_FULL_GLASS_BLOCK_ITEM);
            event.accept(RAINBOW_GALAXY_FULL_GLASS_BLOCK_ITEM);
            event.accept(YELLOW_GALAXY_FULL_GLASS_BLOCK_ITEM);
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
            event.accept(COMPRESSED_ICE_1_ITEM);
            event.accept(COMPRESSED_ICE_2_ITEM);
            event.accept(COMPRESSED_ICE_3_ITEM);
            event.accept(COMPRESSED_ICE_4_ITEM);
            event.accept(COMPRESSED_ICE_5_ITEM);
        }
    }

    private static DeferredBlock<Block> registerCompressedObsidianBlock(String name) {
        return BLOCKS.registerBlock(name, Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN));
    }

    private static DeferredBlock<TransparentBlock> registerCompressedGlassBlock(String name, float destroyTime) {
        return registerTransparentGlassBlock(name, destroyTime);
    }

    private static DeferredBlock<TransparentBlock> registerTransparentGlassBlock(String name, float destroyTime) {
        return BLOCKS.registerBlock(name, TransparentBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(destroyTime, 1200.0F));
    }

    private static DeferredBlock<Block> registerCompressedIceBlock(String name, float destroyTime) {
        return BLOCKS.registerBlock(name, Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_ICE).strength(destroyTime, 1200.0F));
    }

    private static BlockBehaviour.Properties galaxyBlockProperties(Block block) {
        return BlockBehaviour.Properties.ofFullCopy(block).sound(SoundType.STONE).explosionResistance(1200.0F);
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
        updateTheStickCommandTree(player);
        GalaxyInstability.tickPlayerStacks(player);
        boolean changedByStick = player.getPersistentData().getBoolean(THE_STICK_CREATIVE_KEY);
        if (!Config.THE_STICK_GRANTS_CREATIVE.getAsBoolean()) {
            if (changedByStick) {
                if (player.isCreative()) {
                    player.setGameMode(GameType.SURVIVAL);
                }
                player.getPersistentData().remove(THE_STICK_CREATIVE_KEY);
            }
            return;
        }

        boolean hasTheStick = player.getInventory().contains(stack -> stack.is(THE_STICK.get()));
        if (hasTheStick) {
            if (!player.isCreative()) {
                if (player.setGameMode(GameType.CREATIVE)) {
                    player.getPersistentData().putBoolean(THE_STICK_CREATIVE_KEY, true);
                }
            }
            return;
        }

        if (changedByStick) {
            if (player.isCreative()) {
                player.setGameMode(GameType.SURVIVAL);
            }
            player.getPersistentData().remove(THE_STICK_CREATIVE_KEY);
        }
    }

    @SubscribeEvent
    public void onGalaxyToolRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!isGalaxyTool(event.getItemStack()) || !GalaxyToolItem.isHoeMode(event.getItemStack())) {
            return;
        }
        if (event.getLevel().isClientSide) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        useGalaxyHoe(serverLevel, serverPlayer, event.getPos());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void useGalaxyHoe(ServerLevel level, ServerPlayer player, BlockPos origin) {
        BlockState clickedState = level.getBlockState(origin);
        if (clickedState.getBlock() instanceof CropBlock clickedCrop && clickedCrop.isMaxAge(clickedState)) {
            harvestAndReplantGalaxyHoeArea(level, player, origin);
            return;
        }

        boolean growExisting = clickedState.is(Blocks.FARMLAND) || clickedState.getBlock() instanceof CropBlock;
        boolean foundFarmland = false;
        boolean placeWater = !growExisting;
        BlockPos waterPos = galaxyHoeWaterPos(origin, clickedState);
        if (placeWater) {
            placeGalaxyHoeWaterSource(level, player, origin);
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    mutablePos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockPos pos = mutablePos.immutable();
                    if (!level.isInWorldBounds(pos) || !player.mayInteract(level, pos)) {
                        continue;
                    }
                    if (placeWater && pos.equals(waterPos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock cropBlock) {
                        level.setBlock(pos, cropBlock.getStateForAge(cropBlock.getMaxAge()), 3);
                        continue;
                    }

                    if (state.is(Blocks.FARMLAND)) {
                        foundFarmland = true;
                        plantRandomFullCrop(level, player, pos);
                    } else if (!growExisting && canGalaxyHoeReplaceWithFarmland(level, pos)) {
                        level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
                        plantRandomFullCrop(level, player, pos);
                    }
                }
            }
        }

        if (growExisting && !foundFarmland) {
            BlockPos farmlandPos = clickedState.getBlock() instanceof CropBlock ? origin.below() : origin;
            if (level.isInWorldBounds(farmlandPos) && player.mayInteract(level, farmlandPos)) {
                level.setBlock(farmlandPos, Blocks.FARMLAND.defaultBlockState(), 3);
                plantRandomFullCrop(level, player, farmlandPos);
            }
        }
    }

    private static void harvestAndReplantGalaxyHoeArea(ServerLevel level, ServerPlayer player, BlockPos origin) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    mutablePos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockPos pos = mutablePos.immutable();
                    if (!level.isInWorldBounds(pos) || !player.mayInteract(level, pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, ItemStack.EMPTY);
                        for (ItemStack drop : drops) {
                            if (!drop.isEmpty()) {
                                Block.popResource(level, pos, drop);
                            }
                        }
                        level.setBlock(pos, cropBlock.getStateForAge(cropBlock.getMaxAge()), 3);
                    }
                }
            }
        }
    }

    private static void placeGalaxyHoeWaterSource(ServerLevel level, ServerPlayer player, BlockPos origin) {
        BlockPos waterPos = galaxyHoeWaterPos(origin, level.getBlockState(origin));
        if (!level.isInWorldBounds(waterPos) || !player.mayInteract(level, waterPos)) {
            return;
        }
        BlockState state = level.getBlockState(waterPos);
        if (state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, waterPos) < 0.0F) {
            return;
        }
        level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
    }

    private static BlockPos galaxyHoeWaterPos(BlockPos origin, BlockState clickedState) {
        return clickedState.getBlock() instanceof CropBlock ? origin.below() : origin;
    }

    private static boolean canGalaxyHoeReplaceWithFarmland(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());
        return !state.isAir()
                && !(state.getBlock() instanceof CropBlock)
                && state.getDestroySpeed(level, pos) >= 0.0F
                && (aboveState.isAir() || aboveState.canBeReplaced());
    }

    private static void plantRandomFullCrop(ServerLevel level, ServerPlayer player, BlockPos farmlandPos) {
        BlockPos cropPos = farmlandPos.above();
        if (!level.isInWorldBounds(cropPos) || !player.mayInteract(level, cropPos)) {
            return;
        }

        BlockState currentCropState = level.getBlockState(cropPos);
        if (currentCropState.getBlock() instanceof CropBlock cropBlock) {
            level.setBlock(cropPos, cropBlock.getStateForAge(cropBlock.getMaxAge()), 3);
            return;
        }
        if (!currentCropState.isAir() && !currentCropState.canBeReplaced()) {
            return;
        }

        BlockState cropState = randomFullCrop(level);
        if (cropState.canSurvive(level, cropPos)) {
            level.setBlock(cropPos, cropState, 3);
        }
    }

    private static BlockState randomFullCrop(ServerLevel level) {
        CropBlock cropBlock = switch (level.random.nextInt(4)) {
            case 0 -> (CropBlock) Blocks.WHEAT;
            case 1 -> (CropBlock) Blocks.CARROTS;
            case 2 -> (CropBlock) Blocks.POTATOES;
            default -> (CropBlock) Blocks.BEETROOTS;
        };
        return cropBlock.getStateForAge(cropBlock.getMaxAge());
    }

    @SubscribeEvent
    public void onGalaxyToolLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!isGalaxyTool(event.getItemStack())) {
            return;
        }
        if (event.getLevel().isClientSide) {
            event.setCanceled(true);
            return;
        }

        BlockPos pos = event.getPos();
        if (!event.getEntity().mayInteract(event.getLevel(), pos)) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        if (GalaxyToolItem.isPickaxeMode(event.getItemStack()) && event.getLevel() instanceof ServerLevel serverLevel && event.getEntity() instanceof ServerPlayer serverPlayer) {
            breakGalaxyPickaxeCube(serverLevel, serverPlayer, event.getItemStack(), galaxyPickaxeMiningCenter(pos, event.getFace()));
        } else if (GalaxyToolItem.isAxeMode(event.getItemStack()) && isGalaxyAxeTarget(state) && event.getLevel() instanceof ServerLevel serverLevel && event.getEntity() instanceof ServerPlayer serverPlayer) {
            breakGalaxyAxeTree(serverLevel, serverPlayer, pos);
        } else if (GalaxyToolItem.isShovelMode(event.getItemStack()) && state.is(BlockTags.DIRT)) {
            breakGalaxyShovelSpread(event.getLevel(), event.getEntity(), pos);
        } else if (GalaxyToolItem.isAxeMode(event.getItemStack()) || GalaxyToolItem.isHoeMode(event.getItemStack()) || GalaxyToolItem.isShovelMode(event.getItemStack())) {
            event.getLevel().destroyBlock(pos, true, event.getEntity());
        } else {
            event.getLevel().destroyBlock(pos, true, event.getEntity());
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onGalaxyToolBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isGalaxyTool(event.getEntity().getMainHandItem())) {
            event.setNewSpeed(Float.MAX_VALUE);
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (!crafted.is(GALAXY_BLOCK_ITEM.get())) {
            return;
        }

        GalaxyInstability.resetTicks(crafted);
    }

    public static BlockPos galaxyPickaxeMiningCenter(BlockPos targetedPos, Direction face) {
        BlockPos center = targetedPos.relative(face.getOpposite(), 3);
        return face.getAxis().isHorizontal() ? center.above(2) : center;
    }

    public static boolean isGalaxyTool(ItemStack stack) {
        return stack.is(GALAXY_MULTITOOL.get())
                || stack.is(GALAXY_PICKAXE.get())
                || stack.is(GALAXY_AXE.get())
                || stack.is(GALAXY_HOE.get())
                || stack.is(GALAXY_SHOVEL.get())
                || stack.is(GALAXY_SWORD.get());
    }

    private static void breakGalaxyPickaxeCube(ServerLevel level, ServerPlayer player, ItemStack toolStack, BlockPos center) {
        boolean processingEnabled = GalaxyToolItem.pickaxeProcessingEnabled(toolStack);
        ItemStack fortuneTool = galaxyPickaxeLootTool(level, false);
        ItemStack silkTouchTool = processingEnabled ? galaxyPickaxeLootTool(level, true) : ItemStack.EMPTY;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isInWorldBounds(mutablePos) || !player.mayInteract(level, mutablePos)) {
                        continue;
                    }

                    breakGalaxyPickaxeBlock(level, player, mutablePos.immutable(), fortuneTool, silkTouchTool, processingEnabled);
                }
            }
        }
    }

    private static void breakGalaxyPickaxeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack fortuneTool, ItemStack silkTouchTool, boolean processingEnabled) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            if (!level.getFluidState(pos).isEmpty()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            return;
        }

        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        List<ItemStack> fortuneDrops = Block.getDrops(state, level, pos, blockEntity, player, fortuneTool);
        List<ItemStack> silkTouchDrops = processingEnabled
                ? Block.getDrops(state, level, pos, blockEntity, player, silkTouchTool)
                : List.of();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
        for (ItemStack drop : silkTouchDrops) {
            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }
        for (ItemStack drop : fortuneDrops) {
            ItemStack output = processingEnabled ? smeltIfPossible(level, drop) : drop;
            if (!output.isEmpty()) {
                Block.popResource(level, pos, output);
            }
        }
    }

    private static void breakGalaxyAxeTree(ServerLevel level, ServerPlayer player, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> branchQueue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -GALAXY_AXE_RADIUS; x <= GALAXY_AXE_RADIUS; x++) {
            for (int y = -GALAXY_AXE_RADIUS; y <= GALAXY_AXE_RADIUS; y++) {
                for (int z = -GALAXY_AXE_RADIUS; z <= GALAXY_AXE_RADIUS; z++) {
                    mutablePos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockPos pos = mutablePos.immutable();
                    visited.add(pos);
                    if (!level.isInWorldBounds(pos) || !player.mayInteract(level, pos) || !isGalaxyAxeTarget(level.getBlockState(pos))) {
                        continue;
                    }

                    breakGalaxyAxeBlock(level, player, pos);
                    if (Math.abs(x) == GALAXY_AXE_RADIUS || Math.abs(y) == GALAXY_AXE_RADIUS || Math.abs(z) == GALAXY_AXE_RADIUS) {
                        enqueueGalaxyAxeNeighbors(pos, visited, branchQueue);
                    }
                }
            }
        }

        int branchBlocks = 0;
        while (!branchQueue.isEmpty() && branchBlocks < GALAXY_AXE_MAX_BRANCH_BLOCKS) {
            BlockPos pos = branchQueue.remove();
            if (!level.isInWorldBounds(pos) || !player.mayInteract(level, pos) || !isGalaxyAxeTarget(level.getBlockState(pos))) {
                continue;
            }

            breakGalaxyAxeBlock(level, player, pos);
            branchBlocks++;
            enqueueGalaxyAxeNeighbors(pos, visited, branchQueue);
        }
    }

    private static void breakGalaxyAxeBlock(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState saplingState = galaxyAxeSaplingForLog(state);
        boolean shouldReplant = !saplingState.isAir() && canGalaxyAxeReplant(level, pos, saplingState);
        level.destroyBlock(pos, true, player);
        if (shouldReplant && level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, saplingState, 3);
        }
    }

    private static boolean isGalaxyAxeTarget(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    private static boolean canGalaxyAxeReplant(ServerLevel level, BlockPos pos, BlockState saplingState) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT) && saplingState.canSurvive(level, pos);
    }

    private static BlockState galaxyAxeSaplingForLog(BlockState state) {
        if (state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_WOOD) || state.is(Blocks.STRIPPED_OAK_LOG) || state.is(Blocks.STRIPPED_OAK_WOOD)) {
            return Blocks.OAK_SAPLING.defaultBlockState();
        }
        if (state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.SPRUCE_WOOD) || state.is(Blocks.STRIPPED_SPRUCE_LOG) || state.is(Blocks.STRIPPED_SPRUCE_WOOD)) {
            return Blocks.SPRUCE_SAPLING.defaultBlockState();
        }
        if (state.is(Blocks.BIRCH_LOG) || state.is(Blocks.BIRCH_WOOD) || state.is(Blocks.STRIPPED_BIRCH_LOG) || state.is(Blocks.STRIPPED_BIRCH_WOOD)) {
            return Blocks.BIRCH_SAPLING.defaultBlockState();
        }
        if (state.is(Blocks.JUNGLE_LOG) || state.is(Blocks.JUNGLE_WOOD) || state.is(Blocks.STRIPPED_JUNGLE_LOG) || state.is(Blocks.STRIPPED_JUNGLE_WOOD)) {
            return Blocks.JUNGLE_SAPLING.defaultBlockState();
        }
        if (state.is(Blocks.ACACIA_LOG) || state.is(Blocks.ACACIA_WOOD) || state.is(Blocks.STRIPPED_ACACIA_LOG) || state.is(Blocks.STRIPPED_ACACIA_WOOD)) {
            return Blocks.ACACIA_SAPLING.defaultBlockState();
        }
        if (state.is(Blocks.DARK_OAK_LOG) || state.is(Blocks.DARK_OAK_WOOD) || state.is(Blocks.STRIPPED_DARK_OAK_LOG) || state.is(Blocks.STRIPPED_DARK_OAK_WOOD)) {
            return Blocks.DARK_OAK_SAPLING.defaultBlockState();
        }
        if (state.is(Blocks.MANGROVE_LOG) || state.is(Blocks.MANGROVE_WOOD) || state.is(Blocks.STRIPPED_MANGROVE_LOG) || state.is(Blocks.STRIPPED_MANGROVE_WOOD)) {
            return Blocks.MANGROVE_PROPAGULE.defaultBlockState();
        }
        if (state.is(Blocks.CHERRY_LOG) || state.is(Blocks.CHERRY_WOOD) || state.is(Blocks.STRIPPED_CHERRY_LOG) || state.is(Blocks.STRIPPED_CHERRY_WOOD)) {
            return Blocks.CHERRY_SAPLING.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static void enqueueGalaxyAxeNeighbors(BlockPos pos, Set<BlockPos> visited, Queue<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (visited.add(neighbor)) {
                queue.add(neighbor);
            }
        }
    }

    private static ItemStack galaxyPickaxeLootTool(ServerLevel level, boolean processingEnabled) {
        ItemStack lootTool = new ItemStack(Items.DIAMOND_PICKAXE);
        Holder<Enchantment> enchantment = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(processingEnabled ? Enchantments.SILK_TOUCH : Enchantments.FORTUNE);
        lootTool.enchant(enchantment, processingEnabled ? 1 : Integer.MAX_VALUE);
        return lootTool;
    }

    private static ItemStack smeltIfPossible(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SingleRecipeInput input = new SingleRecipeInput(stack);
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, level)
                .map(RecipeHolder::value)
                .map(recipe -> smeltedResult(level, input, recipe, stack.getCount()))
                .filter(result -> !result.isEmpty())
                .orElse(stack);
    }

    private static ItemStack smeltedResult(ServerLevel level, SingleRecipeInput input, AbstractCookingRecipe recipe, int inputCount) {
        ItemStack result = recipe.assemble(input, level.registryAccess()).copy();
        result.setCount(result.getCount() * inputCount);
        return result;
    }

    private static void breakGalaxyShovelSpread(Level level, net.minecraft.world.entity.player.Player player, BlockPos origin) {
        ItemStack silkTouchShovel = level instanceof ServerLevel serverLevel ? galaxySilkTouchShovel(serverLevel) : ItemStack.EMPTY;
        for (int y = -GALAXY_SHOVEL_VERTICAL_RADIUS; y <= GALAXY_SHOVEL_VERTICAL_RADIUS; y++) {
            for (int x = -GALAXY_SHOVEL_HORIZONTAL_RADIUS; x <= GALAXY_SHOVEL_HORIZONTAL_RADIUS; x++) {
                for (int z = -GALAXY_SHOVEL_HORIZONTAL_RADIUS; z <= GALAXY_SHOVEL_HORIZONTAL_RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.isInWorldBounds(pos) || !player.mayInteract(level, pos) || !level.getBlockState(pos).is(BlockTags.DIRT)) {
                        continue;
                    }

                    if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                        breakGalaxySilkTouchBlock(serverLevel, serverPlayer, pos, silkTouchShovel);
                    } else {
                        level.destroyBlock(pos, true, player);
                    }
                }
            }
        }
    }

    private static ItemStack galaxySilkTouchShovel(ServerLevel level) {
        ItemStack lootTool = new ItemStack(Items.DIAMOND_SHOVEL);
        Holder<Enchantment> silkTouch = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        lootTool.enchant(silkTouch, 1);
        return lootTool;
    }

    private static void breakGalaxySilkTouchBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack lootTool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, lootTool);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
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

    private static void updateTheStickCommandTree(ServerPlayer player) {
        boolean hasCommandStick = TheStickItem.grantsServerCommandPermissions(player);
        boolean hadCommandStick = player.getPersistentData().getBoolean(THE_STICK_COMMAND_TREE_KEY);
        if (hasCommandStick == hadCommandStick) {
            return;
        }

        if (hasCommandStick) {
            player.getPersistentData().putBoolean(THE_STICK_COMMAND_TREE_KEY, true);
        } else {
            player.getPersistentData().remove(THE_STICK_COMMAND_TREE_KEY);
        }
        player.server.getCommands().sendCommands(player);
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
        player.displayClientMessage(Component.translatable("message.xavitia.spectator_phase.enter"), true);
    }

    public static boolean handleFabricatorHopperTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !hopperContainsFabricator(hopper)) {
            return false;
        }

        if (!state.is(Blocks.HOPPER) || !state.getValue(HopperBlock.ENABLED)) {
            return true;
        }

        Map<BlockPos, Long> cooldowns = FABRICATOR_HOPPER_COOLDOWNS.computeIfAbsent(serverLevel.dimension(), ignored -> new HashMap<>());
        long gameTime = serverLevel.getGameTime();
        BlockPos immutablePos = pos.immutable();
        if (cooldowns.getOrDefault(immutablePos, 0L) > gameTime) {
            return true;
        }

        if (tryPushFabricatorHopperOutput(serverLevel, immutablePos, state, hopper)) {
            hopper.setCooldown(8);
            cooldowns.put(immutablePos, gameTime + 8L);
            hopper.setChanged();
        }
        return true;
    }

    private static boolean hopperContainsFabricator(HopperBlockEntity hopper) {
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            if (hopper.getItem(slot).getItem() instanceof RandomBlockPlacerItem) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryPushFabricatorHopperOutput(ServerLevel level, BlockPos pos, BlockState state, HopperBlockEntity hopper) {
        ItemStack output = ItemStack.EMPTY;
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack stack = hopper.getItem(slot);
            if (stack.getItem() instanceof RandomBlockPlacerItem) {
                output = RandomBlockPlacerItem.selectedItemStack(stack, 1);
                break;
            }
        }
        if (output.isEmpty()) {
            return false;
        }

        Direction facing = state.getValue(HopperBlock.FACING);
        Direction insertSide = facing.getOpposite();
        BlockPos destinationPos = pos.relative(facing);
        IItemHandler destinationHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, destinationPos, insertSide);
        if (destinationHandler != null && insertIntoHandler(destinationHandler, output)) {
            return true;
        }

        Container container = HopperBlockEntity.getContainerAt(level, destinationPos);
        if (container == null) {
            return false;
        }
        return HopperBlockEntity.addItem(hopper, container, output.copy(), insertSide).isEmpty();
    }

    private static boolean insertIntoHandler(IItemHandler handler, ItemStack stack) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack remainder = handler.insertItem(slot, stack.copy(), false);
            if (remainder.isEmpty()) {
                return true;
            }
        }
        return false;
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
                player.displayClientMessage(Component.translatable("message.xavitia.spectator_phase.countdown", secondsRemaining), true);
                continue;
            }

            ServerLevel originalLevel = server.getLevel(state.dimension());
            ServerLevel targetLevel = originalLevel != null ? originalLevel : player.serverLevel();
            player.teleportTo(targetLevel, state.x(), state.y(), state.z(), state.yRot(), state.xRot());
            player.setGameMode(state.gameType());
            player.displayClientMessage(Component.translatable("message.xavitia.spectator_phase.return"), true);
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
