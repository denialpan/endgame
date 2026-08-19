package com.ddd.endgame.item;

import com.ddd.endgame.item.models.GalaxyMultitoolItemRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class GalaxyMultitoolItem extends Item {
    private static final String TOOL_INDEX_TAG = "GalaxyMultitoolIndex";
    private static final List<String> TOOL_TRANSLATION_KEYS = List.of(
            "item.xavitia.galaxy_multitool.mode.pickaxe",
            "item.xavitia.galaxy_multitool.mode.axe",
            "item.xavitia.galaxy_multitool.mode.hoe",
            "item.xavitia.galaxy_multitool.mode.shovel",
            "item.xavitia.galaxy_multitool.mode.sword"
    );
    private static final List<Item> TOOLS = List.of(
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_AXE,
            Items.DIAMOND_HOE,
            Items.DIAMOND_SHOVEL,
            Items.DIAMOND_SWORD
    );

    public GalaxyMultitoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return GalaxyMultitoolItemRenderer.INSTANCE;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.xavitia.galaxy_multitool.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.translatable("item.xavitia.galaxy_multitool.selected", selectedToolName(stack)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!player.isShiftKeyDown() || !GalaxyToolItem.isPickaxeMode(stack)) {
            return super.use(level, player, usedHand);
        }

        GalaxyToolItem.togglePickaxeProcessing(stack);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    GalaxyToolItem.pickaxeProcessingEnabled(stack)
                            ? "message.xavitia.galaxy_pickaxe.processing.enabled"
                            : "message.xavitia.galaxy_pickaxe.processing.disabled"
            ), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown() && GalaxyToolItem.isPickaxeMode(context.getItemInHand())) {
            return this.use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
        }
        return super.useOn(context);
    }

    public static Item selectedTool(ItemStack stack) {
        return TOOLS.get(Mth.positiveModulo(selectedIndex(stack), TOOLS.size()));
    }

    public static int selectedToolIndex(ItemStack stack) {
        return Mth.positiveModulo(selectedIndex(stack), TOOLS.size());
    }

    public static Item cycleSelectedTool(ItemStack stack, int direction) {
        int step = direction >= 0 ? 1 : -1;
        int index = Mth.positiveModulo(selectedIndex(stack) + step, TOOLS.size());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TOOL_INDEX_TAG, index));
        return TOOLS.get(index);
    }

    public static Component selectedToolName(ItemStack stack) {
        return toolName(selectedToolIndex(stack));
    }

    public static Component toolName(int index) {
        return Component.translatable(TOOL_TRANSLATION_KEYS.get(Mth.positiveModulo(index, TOOL_TRANSLATION_KEYS.size())));
    }

    private static int selectedIndex(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.copyTag().contains(TOOL_INDEX_TAG)) {
            return customData.copyTag().getInt(TOOL_INDEX_TAG);
        }
        return 0;
    }
}
