package com.ddd.endgame.item;

import com.ddd.endgame.item.models.GalaxyMultitoolItemRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class GalaxyToolItem extends Item {
    public static final int PICKAXE_INDEX = 0;
    public static final int AXE_INDEX = 1;
    public static final int HOE_INDEX = 2;
    public static final int SHOVEL_INDEX = 3;
    public static final int SWORD_INDEX = 4;
    private static final String PICKAXE_PROCESSING_TAG = "GalaxyPickaxeProcessing";
    private final int toolIndex;

    public GalaxyToolItem(int toolIndex, Properties properties) {
        super(properties);
        this.toolIndex = toolIndex;
    }

    public int toolIndex() {
        return this.toolIndex;
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
        tooltipComponents.add(Component.translatable(tooltipKey(stack)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static String tooltipKey(ItemStack stack) {
        return switch (toolIndex(stack)) {
            case PICKAXE_INDEX -> "item.xavitia.galaxy_pickaxe.tooltip";
            case AXE_INDEX -> "item.xavitia.galaxy_axe.tooltip";
            case HOE_INDEX -> "item.xavitia.galaxy_hoe.tooltip";
            case SHOVEL_INDEX -> "item.xavitia.galaxy_shovel.tooltip";
            case SWORD_INDEX -> "item.xavitia.galaxy_sword.tooltip";
            default -> "item.xavitia.galaxy_tool.tooltip";
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!player.isShiftKeyDown() || !isPickaxeMode(stack)) {
            return super.use(level, player, usedHand);
        }

        togglePickaxeProcessing(stack);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    pickaxeProcessingEnabled(stack)
                            ? "message.xavitia.galaxy_pickaxe.processing.enabled"
                            : "message.xavitia.galaxy_pickaxe.processing.disabled"
            ), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown() && isPickaxeMode(context.getItemInHand())) {
            return this.use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
        }
        return super.useOn(context);
    }

    public static boolean isPickaxeMode(ItemStack stack) {
        return toolIndex(stack) == PICKAXE_INDEX;
    }

    public static boolean isAxeMode(ItemStack stack) {
        return toolIndex(stack) == AXE_INDEX;
    }

    public static boolean isShovelMode(ItemStack stack) {
        return toolIndex(stack) == SHOVEL_INDEX;
    }

    public static boolean isHoeMode(ItemStack stack) {
        return toolIndex(stack) == HOE_INDEX;
    }

    public static boolean isSwordMode(ItemStack stack) {
        return toolIndex(stack) == SWORD_INDEX;
    }

    public static int toolIndex(ItemStack stack) {
        if (stack.getItem() instanceof GalaxyToolItem toolItem) {
            return toolItem.toolIndex();
        }
        return stack.getItem() instanceof GalaxyMultitoolItem ? GalaxyMultitoolItem.selectedToolIndex(stack) : -1;
    }

    public static boolean pickaxeProcessingEnabled(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(PICKAXE_PROCESSING_TAG);
    }

    public static void togglePickaxeProcessing(ItemStack stack) {
        boolean enabled = !pickaxeProcessingEnabled(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(PICKAXE_PROCESSING_TAG, enabled));
    }
}
