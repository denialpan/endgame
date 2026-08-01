package com.ddd.endgame;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.MapColor;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(dddsendgame.MODID)
public class dddsendgame {
    public static final String MODID = "dddsendgame";
    public static final long ENDGAME_ITEM_REQUIREMENT = 1_048_576L;
    private static final String ENDGAME_STICK_CREATIVE_KEY = MODID + ".endgame_stick_creative";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredItem<Item> ENDGAME_TEST_STICK = ITEMS.registerSimpleItem("endgame_test_stick", new Item.Properties().stacksTo(1));
    public static final DeferredBlock<EndgameTemplateBlock> ENDGAME_TEMPLATE_BLOCK = BLOCKS.registerBlock(
            "endgame_template",
            EndgameTemplateBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
    );
    public static final DeferredItem<BlockItem> ENDGAME_TEMPLATE_ITEM = ITEMS.register(
            "endgame_template",
            () -> new EndgameTemplateBlockItem(ENDGAME_TEMPLATE_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredBlock<EndgameTemplateInputBlock> ENDGAME_TEMPLATE_INPUT_BLOCK = BLOCKS.registerBlock(
            "endgame_template_input",
            EndgameTemplateInputBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).sound(SoundType.STONE)
    );
    public static final DeferredItem<BlockItem> ENDGAME_TEMPLATE_INPUT_ITEM = ITEMS.register(
            "endgame_template_input",
            () -> new EndgameTemplateBlockItem(ENDGAME_TEMPLATE_INPUT_BLOCK.get(), new Item.Properties())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndgameTemplateBlockEntity>> ENDGAME_TEMPLATE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("endgame_template", () -> BlockEntityType.Builder.of(EndgameTemplateBlockEntity::new, ENDGAME_TEMPLATE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndgameTemplateInputBlockEntity>> ENDGAME_TEMPLATE_INPUT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("endgame_template_input", () -> BlockEntityType.Builder.of(EndgameTemplateInputBlockEntity::new, ENDGAME_TEMPLATE_INPUT_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<EndgameTemplateMenu>> ENDGAME_TEMPLATE_MENU =
            MENU_TYPES.register("endgame_template", () -> IMenuTypeExtension.create(EndgameTemplateMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dddsendgame"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ENDGAME_TEMPLATE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                output.accept(EXAMPLE_BLOCK_ITEM.get());
                output.accept(ENDGAME_TEMPLATE_ITEM.get());
                output.accept(ENDGAME_TEMPLATE_INPUT_ITEM.get());
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
                ENDGAME_TEMPLATE_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ENDGAME_TEMPLATE_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ENDGAME_TEMPLATE_INPUT_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ENDGAME_TEMPLATE_INPUT_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
            event.accept(ENDGAME_TEMPLATE_ITEM);
            event.accept(ENDGAME_TEMPLATE_INPUT_ITEM);
        }
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
}
