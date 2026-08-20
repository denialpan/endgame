package com.ddd.endgame.block;

import com.ddd.endgame.galaxy.GalaxyInstability;
import com.ddd.endgame.galaxy.GalaxyTooltip;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class GalaxyBlockItem extends EndgameSkyboxBlockItem {
    public GalaxyBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player)) {
            GalaxyInstability.tickStack(stack, level, entity);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return GalaxyInstability.tickDroppedStack(stack, entity);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "block.xavitia.galaxy_block.tooltip",
                Component.literal(String.valueOf(GalaxyTooltip.remainingSeconds(stack))).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        ).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
