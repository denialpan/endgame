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
    public static final int HOE_INDEX = 2;
    public static final int SHOVEL_INDEX = 3;
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
        tooltipComponents.add(Component.translatable("item.xavitia.galaxy_tool.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (isPickaxeMode(stack)) {
            tooltipComponents.add(Component.translatable(
                    pickaxeProcessingEnabled(stack)
                            ? "item.xavitia.galaxy_tool.pickaxe_processing.enabled"
                            : "item.xavitia.galaxy_tool.pickaxe_processing.disabled"
            ).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
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

    public static boolean isShovelMode(ItemStack stack) {
        return toolIndex(stack) == SHOVEL_INDEX;
    }

    public static boolean isHoeMode(ItemStack stack) {
        return toolIndex(stack) == HOE_INDEX;
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
