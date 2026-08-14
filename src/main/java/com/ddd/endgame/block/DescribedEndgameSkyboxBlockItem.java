package com.ddd.endgame.block;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class DescribedEndgameSkyboxBlockItem extends EndgameSkyboxBlockItem {
    private final String tooltipKey;

    public DescribedEndgameSkyboxBlockItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
